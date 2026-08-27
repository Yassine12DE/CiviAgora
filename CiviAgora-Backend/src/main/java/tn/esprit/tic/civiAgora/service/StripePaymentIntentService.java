package tn.esprit.tic.civiAgora.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.config.StripeIntegrationConfig;
import tn.esprit.tic.civiAgora.dao.entity.StripeCheckoutSessionRecord;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutFlow;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionBillingCycle;
import tn.esprit.tic.civiAgora.dao.repository.StripeCheckoutSessionRepository;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionCreateRequestDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripePaymentIntentDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripePaymentIntentService {

    private final StripeIntegrationConfig stripeIntegrationConfig;
    private final StripeCheckoutService stripeCheckoutService;
    private final StripeCheckoutSessionRepository sessionRepository;
    private final OrganizationRequestService organizationRequestService;
    private final OrganizationBillingService organizationBillingService;

    @Transactional
    public StripePaymentIntentDto createPaymentIntent(StripeCheckoutSessionCreateRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Stripe payment request is required");
        }
        if (request.getFlowType() == null) {
            throw new IllegalArgumentException("Payment flow is required");
        }

        Stripe.apiKey = stripeIntegrationConfig.requireSecretKey();
        String publishableKey = stripeIntegrationConfig.requirePublishableKey();
        StripeCheckoutService.PreparedCheckout preparedCheckout = stripeCheckoutService.prepareCheckout(request);

        try {
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(toStripeAmount(preparedCheckout.amount()))
                    .setCurrency(stripeIntegrationConfig.getCurrency())
                    .setDescription(preparedCheckout.description())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );

            if (preparedCheckout.customerEmail() != null && !preparedCheckout.customerEmail().isBlank()) {
                paramsBuilder.setReceiptEmail(preparedCheckout.customerEmail());
            }

            PaymentIntentCreateParams params = paramsBuilder
                    .putMetadata("integration", "embedded_elements")
                    .putMetadata("flowType", request.getFlowType().name())
                    .putMetadata("organizationName", safe(preparedCheckout.organizationName()))
                    .putMetadata("organizationSlug", safe(preparedCheckout.organizationSlug()))
                    .putMetadata("organizationRequestId", preparedCheckout.organizationRequestId() == null ? "" : String.valueOf(preparedCheckout.organizationRequestId()))
                    .putMetadata("organizationId", preparedCheckout.organizationId() == null ? "" : String.valueOf(preparedCheckout.organizationId()))
                    .putMetadata("planCode", safe(preparedCheckout.planCode()))
                    .putMetadata("moduleCode", safe(preparedCheckout.moduleCode()))
                    .putMetadata("billingCycle", preparedCheckout.billingCycle() == null ? "" : preparedCheckout.billingCycle().name())
                    .putMetadata("subscriptionAction", safe(preparedCheckout.subscriptionAction()))
                    .putMetadata("moduleSummary", safe(preparedCheckout.moduleSummary()))
                    .putMetadata("referenceToken", safe(preparedCheckout.referenceToken()))
                    .putMetadata("customerName", safe(preparedCheckout.customerName()))
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            LocalDateTime now = LocalDateTime.now();

            // The legacy checkout table also stores embedded PaymentIntent records to keep reporting intact.
            StripeCheckoutSessionRecord record = StripeCheckoutSessionRecord.builder()
                    .stripeSessionId(paymentIntent.getId())
                    .stripePaymentIntentId(paymentIntent.getId())
                    .flowType(request.getFlowType())
                    .referenceToken(preparedCheckout.referenceToken())
                    .organizationRequestId(preparedCheckout.organizationRequestId())
                    .organizationId(preparedCheckout.organizationId())
                    .organizationName(preparedCheckout.organizationName())
                    .organizationSlug(preparedCheckout.organizationSlug())
                    .planCode(preparedCheckout.planCode())
                    .moduleCode(preparedCheckout.moduleCode())
                    .billingCycle(preparedCheckout.billingCycle())
                    .subscriptionAction(preparedCheckout.subscriptionAction())
                    .moduleSummary(preparedCheckout.moduleSummary())
                    .paymentStatus(StripeCheckoutStatus.OPEN)
                    .currency(stripeIntegrationConfig.getCurrency())
                    .amount(preparedCheckout.amount())
                    .customerEmail(preparedCheckout.customerEmail())
                    .customerName(preparedCheckout.customerName())
                    .description(preparedCheckout.description())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            StripeCheckoutSessionRecord saved = sessionRepository.save(record);
            return toDto(saved, paymentIntent.getClientSecret(), publishableKey);
        } catch (StripeException exception) {
            throw new IllegalStateException("Stripe PaymentIntent could not be created: " + rootMessage(exception), exception);
        }
    }

    @Transactional
    public StripePaymentIntentDto syncPaymentIntent(String paymentIntentId) {
        String normalizedPaymentIntentId = normalize(paymentIntentId);
        if (normalizedPaymentIntentId.isBlank()) {
            throw new IllegalArgumentException("Stripe PaymentIntent id is required");
        }

        Stripe.apiKey = stripeIntegrationConfig.requireSecretKey();
        String publishableKey = stripeIntegrationConfig.requirePublishableKey();

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(normalizedPaymentIntentId);
            StripeCheckoutSessionRecord record = findPaymentIntentRecord(normalizedPaymentIntentId);
            StripeCheckoutStatus resolvedStatus = toRecordStatus(paymentIntent.getStatus());
            boolean wasAlreadyCompleted = record.getPaymentStatus() == StripeCheckoutStatus.COMPLETED
                    && record.getCompletedAt() != null;
            LocalDateTime now = LocalDateTime.now();

            record.setPaymentStatus(resolvedStatus);
            record.setStripePaymentIntentId(paymentIntent.getId());
            record.setUpdatedAt(now);
            if (resolvedStatus == StripeCheckoutStatus.COMPLETED && record.getCompletedAt() == null) {
                record.setCompletedAt(now);
            }
            sessionRepository.save(record);

            if (resolvedStatus == StripeCheckoutStatus.COMPLETED && !wasAlreadyCompleted) {
                finalizeSuccessfulPayment(record, paymentIntent);
            } else if (resolvedStatus == StripeCheckoutStatus.FAILED || resolvedStatus == StripeCheckoutStatus.CANCELED) {
                finalizeFailedPayment(record);
            }

            StripeCheckoutSessionRecord refreshed = findPaymentIntentRecord(normalizedPaymentIntentId);
            return toDto(refreshed, paymentIntent.getClientSecret(), publishableKey);
        } catch (StripeException exception) {
            throw new IllegalStateException("Stripe PaymentIntent could not be refreshed: " + rootMessage(exception), exception);
        }
    }

    private void finalizeSuccessfulPayment(StripeCheckoutSessionRecord record, PaymentIntent paymentIntent) {
        if (record.getFlowType() == StripeCheckoutFlow.ORGANIZATION_REQUEST) {
            if (record.getOrganizationRequestId() == null) {
                return;
            }
            organizationRequestService.markPaymentCompleted(
                    record.getOrganizationRequestId(),
                    "stripe-payment-intent",
                    record.getReferenceToken(),
                    paymentIntent.getId()
            );
            return;
        }

        if (record.getOrganizationId() == null) {
            return;
        }

        String customerEmail = firstNonBlank(paymentIntent.getReceiptEmail(), record.getCustomerEmail());
        String customerName = firstNonBlank(metadataValue(paymentIntent.getMetadata(), "customerName"), record.getCustomerName());

        if (record.getFlowType() == StripeCheckoutFlow.SUBSCRIPTION) {
            organizationBillingService.markSubscriptionPaymentSuccess(
                    record.getOrganizationId(),
                    firstNonBlank(metadataValue(paymentIntent.getMetadata(), "planCode"), record.getPlanCode()),
                    resolveBillingCycle(metadataValue(paymentIntent.getMetadata(), "billingCycle"), record.getBillingCycle()),
                    firstNonBlank(metadataValue(paymentIntent.getMetadata(), "subscriptionAction"), record.getSubscriptionAction()),
                    paymentIntent.getId(),
                    paymentIntent.getId(),
                    customerEmail,
                    customerName
            );
            return;
        }

        if (record.getFlowType() == StripeCheckoutFlow.MODULE_PURCHASE) {
            organizationBillingService.markModulePurchasePaymentSuccess(
                    record.getOrganizationId(),
                    firstNonBlank(metadataValue(paymentIntent.getMetadata(), "moduleCode"), record.getModuleCode()),
                    paymentIntent.getId(),
                    paymentIntent.getId(),
                    customerEmail,
                    customerName
            );
        }
    }

    private void finalizeFailedPayment(StripeCheckoutSessionRecord record) {
        if (record.getOrganizationId() == null) {
            return;
        }
        if (record.getFlowType() == StripeCheckoutFlow.SUBSCRIPTION) {
            organizationBillingService.markSubscriptionPaymentFailed(record.getOrganizationId(), record.getStripeSessionId());
        }
        if (record.getFlowType() == StripeCheckoutFlow.MODULE_PURCHASE && record.getModuleCode() != null) {
            organizationBillingService.markModulePurchasePaymentFailed(
                    record.getOrganizationId(),
                    record.getModuleCode(),
                    record.getStripeSessionId()
            );
        }
    }

    private StripeCheckoutSessionRecord findPaymentIntentRecord(String paymentIntentId) {
        return sessionRepository.findByStripePaymentIntentId(paymentIntentId)
                .or(() -> sessionRepository.findByStripeSessionId(paymentIntentId))
                .orElseThrow(() -> new IllegalArgumentException("Stripe PaymentIntent record not found"));
    }

    private StripeCheckoutStatus toRecordStatus(String stripeStatus) {
        return switch (normalize(stripeStatus).toLowerCase(Locale.ROOT)) {
            case "succeeded" -> StripeCheckoutStatus.COMPLETED;
            case "canceled" -> StripeCheckoutStatus.CANCELED;
            case "requires_payment_method" -> StripeCheckoutStatus.FAILED;
            default -> StripeCheckoutStatus.OPEN;
        };
    }

    private SubscriptionBillingCycle resolveBillingCycle(String metadataBillingCycle, SubscriptionBillingCycle fallback) {
        if (metadataBillingCycle == null || metadataBillingCycle.isBlank()) {
            return fallback == null ? SubscriptionBillingCycle.MONTHLY : fallback;
        }
        try {
            return SubscriptionBillingCycle.valueOf(metadataBillingCycle.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback == null ? SubscriptionBillingCycle.MONTHLY : fallback;
        }
    }

    private StripePaymentIntentDto toDto(StripeCheckoutSessionRecord record, String clientSecret, String publishableKey) {
        return StripePaymentIntentDto.builder()
                .id(record.getId())
                .stripePaymentIntentId(record.getStripePaymentIntentId())
                .clientSecret(clientSecret)
                .publishableKey(publishableKey)
                .flowType(record.getFlowType())
                .referenceToken(record.getReferenceToken())
                .organizationRequestId(record.getOrganizationRequestId())
                .organizationId(record.getOrganizationId())
                .organizationName(record.getOrganizationName())
                .organizationSlug(record.getOrganizationSlug())
                .planCode(record.getPlanCode())
                .moduleCode(record.getModuleCode())
                .billingCycle(record.getBillingCycle())
                .subscriptionAction(record.getSubscriptionAction())
                .moduleSummary(record.getModuleSummary())
                .paymentStatus(record.getPaymentStatus())
                .currency(record.getCurrency())
                .amount(record.getAmount())
                .customerEmail(record.getCustomerEmail())
                .customerName(record.getCustomerName())
                .description(record.getDescription())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .completedAt(record.getCompletedAt())
                .build();
    }

    private long toStripeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required for Stripe payment");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String firstNonBlank(String primary, String fallback) {
        String normalizedPrimary = normalize(primary);
        return normalizedPrimary.isBlank() ? normalize(fallback) : normalizedPrimary;
    }

    private String metadataValue(Map<String, String> metadata, String key) {
        if (metadata == null || key == null || key.isBlank()) {
            return null;
        }
        return metadata.get(key);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getMessage() : current.getMessage();
    }
}
