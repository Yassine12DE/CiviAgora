package tn.esprit.tic.civiAgora.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.config.StripeIntegrationConfig;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;
import tn.esprit.tic.civiAgora.dao.entity.StripeCheckoutSessionRecord;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.PaymentStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutFlow;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionBillingCycle;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRequestRepository;
import tn.esprit.tic.civiAgora.dao.repository.StripeCheckoutSessionRepository;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionCreateRequestDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripeCheckoutService {

    private final StripeIntegrationConfig stripeIntegrationConfig;
    private final StripeCheckoutSessionRepository sessionRepository;
    private final OrganizationRequestRepository requestRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationRequestService organizationRequestService;
    private final ModuleService moduleService;
    private final OrganizationBillingService organizationBillingService;
    private final BillingPricingService billingPricingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public StripeCheckoutSessionDto createCheckoutSession(StripeCheckoutSessionCreateRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Stripe checkout request is required");
        }

        StripeCheckoutFlow flowType = request.getFlowType();
        if (flowType == null) {
            throw new IllegalArgumentException("Checkout flow is required");
        }

        Stripe.apiKey = stripeIntegrationConfig.requireSecretKey();

        PreparedCheckout preparedCheckout = prepareCheckout(request);

        try {
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(buildRedirectUrl(
                            stripeIntegrationConfig.getSuccessUrl(),
                            preparedCheckout.sessionContext(),
                            true
                    ))
                    .setCancelUrl(buildRedirectUrl(
                            stripeIntegrationConfig.getCancelUrl(),
                            preparedCheckout.sessionContext(),
                            false
                    ))
                    .setClientReferenceId(preparedCheckout.clientReferenceId())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(stripeIntegrationConfig.getCurrency())
                                                    .setUnitAmount(toStripeAmount(preparedCheckout.amount()))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(preparedCheckout.productName())
                                                                    .setDescription(preparedCheckout.description())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    );

            if (preparedCheckout.customerEmail() != null && !preparedCheckout.customerEmail().isBlank()) {
                paramsBuilder.setCustomerEmail(preparedCheckout.customerEmail());
            }

            SessionCreateParams params = paramsBuilder
                    .putMetadata("flowType", flowType.name())
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
                    .build();

            Session session = Session.create(params);
            LocalDateTime now = LocalDateTime.now();

            StripeCheckoutSessionRecord record = StripeCheckoutSessionRecord.builder()
                    .stripeSessionId(session.getId())
                    .flowType(flowType)
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
                    .checkoutUrl(session.getUrl())
                    .description(preparedCheckout.description())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            StripeCheckoutSessionRecord saved = sessionRepository.save(record);
            return toDto(saved);
        } catch (StripeException exception) {
            throw new IllegalStateException("Stripe Checkout session could not be created: " + rootMessage(exception), exception);
        }
    }

    public StripeCheckoutSessionDto getCheckoutSessionByStripeSessionId(String stripeSessionId) {
        StripeCheckoutSessionRecord record = sessionRepository.findByStripeSessionId(normalize(stripeSessionId))
                .orElseThrow(() -> new IllegalArgumentException("Stripe Checkout session not found"));
        return toDto(record);
    }

    @Transactional
    public StripeCheckoutSessionDto refreshCheckoutSession(String stripeSessionId) {
        if (stripeSessionId == null || stripeSessionId.isBlank()) {
            throw new IllegalArgumentException("Stripe session id is required");
        }

        Stripe.apiKey = stripeIntegrationConfig.requireSecretKey();
        String normalizedSessionId = normalize(stripeSessionId);

        try {
            Session session = Session.retrieve(normalizedSessionId);
            StripeCheckoutSessionRecord record = sessionRepository.findByStripeSessionId(normalizedSessionId)
                    .orElseThrow(() -> new IllegalStateException("Stripe session record not found for " + normalizedSessionId));

            syncStripeSessionRecord(
                    record,
                    session.getPaymentIntent(),
                    session.getPaymentStatus(),
                    normalizedSessionId
            );
            maybeFinalizeCheckout(
                    record,
                    extractMetadataValue(session.getMetadata(), "paymentToken"),
                    extractMetadataValue(session.getMetadata(), "organizationRequestId"),
                    extractMetadataValue(session.getMetadata(), "moduleCode"),
                    extractMetadataValue(session.getMetadata(), "planCode"),
                    extractMetadataValue(session.getMetadata(), "billingCycle"),
                    extractMetadataValue(session.getMetadata(), "subscriptionAction"),
                    normalizedSessionId
                    ,
                    session.getPaymentIntent(),
                    session.getCustomerEmail(),
                    session.getCustomerDetails() != null ? session.getCustomerDetails().getName() : null,
                    "paid".equalsIgnoreCase(session.getPaymentStatus())
            );
            return getCheckoutSessionByStripeSessionId(normalizedSessionId);
        } catch (StripeException exception) {
            throw new IllegalStateException("Stripe checkout session could not be refreshed: " + rootMessage(exception), exception);
        }
    }

    @Transactional
    public void handleCheckoutSessionCompleted(String payload, String signatureHeader) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Webhook payload is required");
        }

        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Stripe-Signature header is required");
        }

        String webhookSecret = stripeIntegrationConfig.requireWebhookSecret();
        try {
            com.stripe.model.Event event = com.stripe.net.Webhook.constructEvent(payload, signatureHeader, webhookSecret);
            if (!"checkout.session.completed".equals(event.getType())) {
                return;
            }

            JsonNode root = objectMapper.readTree(payload);
            JsonNode sessionNode = root.path("data").path("object");
            String sessionId = sessionNode.path("id").asText(null);
            if (sessionId == null || sessionId.isBlank()) {
                throw new IllegalStateException("Stripe webhook payload is missing the Checkout Session id");
            }

            StripeCheckoutSessionRecord record = sessionRepository.findByStripeSessionId(sessionId)
                    .orElseThrow(() -> new IllegalStateException("Stripe session record not found for " + sessionId));

            syncStripeSessionRecord(
                    record,
                    sessionNode.path("payment_intent").asText(null),
                    sessionNode.path("payment_status").asText(null),
                    sessionId
            );
            maybeFinalizeCheckout(
                    record,
                    extractText(sessionNode.path("metadata"), "paymentToken"),
                    extractText(sessionNode.path("metadata"), "organizationRequestId"),
                    extractText(sessionNode.path("metadata"), "moduleCode"),
                    extractText(sessionNode.path("metadata"), "planCode"),
                    extractText(sessionNode.path("metadata"), "billingCycle"),
                    extractText(sessionNode.path("metadata"), "subscriptionAction"),
                    sessionId
                    ,
                    sessionNode.path("payment_intent").asText(null),
                    sessionNode.path("customer_email").asText(null),
                    sessionNode.path("customer_details").path("name").asText(null),
                    "paid".equalsIgnoreCase(sessionNode.path("payment_status").asText(null))
            );
        } catch (com.stripe.exception.SignatureVerificationException exception) {
            throw new IllegalArgumentException("Stripe webhook signature verification failed", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Stripe webhook could not be processed: " + rootMessage(exception), exception);
        }
    }

    PreparedCheckout prepareCheckout(StripeCheckoutSessionCreateRequestDto request) {
        if (request.getFlowType() == StripeCheckoutFlow.ORGANIZATION_REQUEST) {
            return prepareOrganizationRequestCheckout(request);
        }
        if (request.getFlowType() == StripeCheckoutFlow.SUBSCRIPTION) {
            return prepareSubscriptionCheckout(request);
        }
        if (request.getFlowType() == StripeCheckoutFlow.MODULE_PURCHASE) {
            return prepareModulePurchaseCheckout(request);
        }
        throw new IllegalArgumentException("Unsupported Stripe checkout flow");
    }

    private PreparedCheckout prepareOrganizationRequestCheckout(StripeCheckoutSessionCreateRequestDto request) {
        if (request.getPaymentToken() == null || request.getPaymentToken().isBlank()) {
            throw new IllegalArgumentException("Payment token is required for organization request checkout");
        }

        OrganizationRequest organizationRequest = requestRepository.findByPaymentTokenHash(hashToken(request.getPaymentToken()))
                .orElseThrow(() -> new IllegalArgumentException("Payment link is invalid or expired"));

        if (organizationRequest.getQuoteTotal() == null) {
            throw new IllegalStateException("A quote must be available before Stripe checkout can be created");
        }

        if (organizationRequest.getPaymentStatus() != PaymentStatus.AWAITING_PAYMENT
                && organizationRequest.getRequestStatus() != OrganizationRequestStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Approve the request before starting Stripe checkout");
        }

        String moduleSummary = organizationRequest.getRequestedModuleCodes() == null || organizationRequest.getRequestedModuleCodes().isEmpty()
                ? "Default Civox module set"
                : String.join(", ", organizationRequest.getRequestedModuleCodes());

        return new PreparedCheckout(
                organizationRequest.getOrganizationName(),
                organizationRequest.getDesiredSlug(),
                organizationRequest.getContactEmail(),
                organizationRequest.getContactPersonName(),
                organizationRequest.getQuoteTotal(),
                "Civox organization activation",
                buildDescription("Organization request", organizationRequest.getOrganizationName(), moduleSummary),
                "request-" + organizationRequest.getId(),
                organizationRequest.getId(),
                null,
                null,
                null,
                null,
                null,
                moduleSummary,
                request.getPaymentToken(),
                sessionContext(request.getPaymentToken(), organizationRequest.getId(), null, null, null, null, null, organizationRequest.getDesiredSlug(), organizationRequest.getOrganizationName())
        );
    }

    private PreparedCheckout prepareSubscriptionCheckout(StripeCheckoutSessionCreateRequestDto request) {
        if (request.getOrganizationId() == null) {
            throw new IllegalArgumentException("Organization id is required for subscription checkout");
        }
        if (request.getPlanCode() == null || request.getPlanCode().isBlank()) {
            throw new IllegalArgumentException("Plan code is required for subscription checkout");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID " + request.getOrganizationId()));

        SubscriptionBillingCycle billingCycle = request.getBillingCycle() == null
                ? SubscriptionBillingCycle.MONTHLY
                : request.getBillingCycle();
        BigDecimal amount = billingPricingService.resolveSubscriptionPrice(request.getPlanCode(), billingCycle);
        String planCode = normalize(request.getPlanCode()).toUpperCase(Locale.ROOT);
        String description = buildDescription("Subscription checkout", organization.getName(), planCode + " / " + billingCycle.name().toLowerCase(Locale.ROOT));

        return new PreparedCheckout(
                organization.getName(),
                organization.getSlug(),
                organization.getEmail(),
                organization.getName(),
                amount,
                "Civox subscription renewal",
                description,
                "subscription-" + organization.getId(),
                null,
                organization.getId(),
                planCode,
                null,
                billingCycle,
                request.getSubscriptionAction() == null ? "RENEW" : request.getSubscriptionAction().trim().toUpperCase(Locale.ROOT),
                null,
                "subscription-" + organization.getId(),
                sessionContext(null, null, organization.getId(), planCode, null, billingCycle, request.getSubscriptionAction(), organization.getSlug(), organization.getName())
        );
    }

    private PreparedCheckout prepareModulePurchaseCheckout(StripeCheckoutSessionCreateRequestDto request) {
        if (request.getOrganizationId() == null) {
            throw new IllegalArgumentException("Organization id is required for module purchase checkout");
        }
        if (request.getModuleCode() == null || request.getModuleCode().isBlank()) {
            throw new IllegalArgumentException("Module code is required for module purchase checkout");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID " + request.getOrganizationId()));

        Module module = moduleService.getModuleByCode(request.getModuleCode());
        organizationBillingService.createModulePurchaseRequest(
                organization.getId(),
                module.getCode(),
                "Stripe checkout for module purchase"
        );

        BigDecimal amount = resolveModuleAmount(module, request.getBillingCycle());
        SubscriptionBillingCycle billingCycle = request.getBillingCycle();
        String description = buildDescription("Module purchase", organization.getName(), module.getName());

        return new PreparedCheckout(
                organization.getName(),
                organization.getSlug(),
                organization.getEmail(),
                organization.getName(),
                amount,
                module.getName(),
                description,
                "module-" + organization.getId() + "-" + module.getCode(),
                null,
                organization.getId(),
                null,
                module.getCode(),
                billingCycle,
                "PURCHASE",
                module.getName(),
                "module-" + organization.getId() + "-" + module.getCode(),
                sessionContext(null, null, organization.getId(), null, module.getCode(), billingCycle, "PURCHASE", organization.getSlug(), organization.getName())
        );
    }

    private BigDecimal resolveModuleAmount(tn.esprit.tic.civiAgora.dao.entity.Module module, SubscriptionBillingCycle billingCycle) {
        if (module == null) {
            throw new IllegalArgumentException("Module not found");
        }

        BigDecimal amount = switch (billingCycle == null ? SubscriptionBillingCycle.MONTHLY : billingCycle) {
            case YEARLY -> module.getYearlyPrice();
            case MONTHLY -> module.getMonthlyPrice();
        };

        if (amount == null || amount.signum() <= 0) {
            amount = module.getOneTimePrice();
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalStateException("No price configured for module " + module.getCode());
        }

        return amount;
    }

    private String buildDescription(String title, String organizationName, String detail) {
        return title + " for " + safe(organizationName) + (detail == null || detail.isBlank() ? "" : " | " + detail);
    }

    private void syncStripeSessionRecord(StripeCheckoutSessionRecord record, String paymentIntentId, String paymentStatus, String fallbackSessionId) {
        if (record == null) {
            return;
        }

        if (paymentIntentId != null && !paymentIntentId.isBlank()) {
            record.setStripePaymentIntentId(paymentIntentId);
        }
        if ("paid".equalsIgnoreCase(paymentStatus)) {
            record.setPaymentStatus(StripeCheckoutStatus.COMPLETED);
            if (record.getCompletedAt() == null) {
                record.setCompletedAt(LocalDateTime.now());
            }
        }
        record.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(record);
    }

    private void maybeFinalizeCheckout(
            StripeCheckoutSessionRecord record,
            String paymentToken,
            String metadataRequestId,
            String metadataModuleCode,
            String metadataPlanCode,
            String metadataBillingCycle,
            String metadataSubscriptionAction,
            String fallbackSessionId,
            String paymentIntentId,
            String customerEmail,
            String customerName,
            boolean paid
    ) {
        if (record == null) {
            return;
        }

        String stripeSessionId = fallbackSessionId != null && !fallbackSessionId.isBlank()
                ? fallbackSessionId
                : record.getStripeSessionId();

        if (record.getFlowType() == StripeCheckoutFlow.ORGANIZATION_REQUEST) {
            Integer requestId = record.getOrganizationRequestId();
            if (metadataRequestId != null && !metadataRequestId.isBlank()) {
                try {
                    requestId = Integer.valueOf(metadataRequestId);
                } catch (NumberFormatException ignored) {
                    // Keep repository-backed request id if metadata is malformed.
                }
            }

            if (requestId == null) {
                return;
            }

            OrganizationRequest request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalStateException("Organization request not found for Stripe session " + stripeSessionId));

            if (request.getPaymentStatus() == PaymentStatus.PAID && request.getRequestStatus() == OrganizationRequestStatus.APPROVED) {
                if (request.getStripeSessionId() == null || request.getStripeSessionId().isBlank()) {
                    request.setStripeSessionId(stripeSessionId);
                    request.setUpdatedAt(LocalDateTime.now());
                    requestRepository.save(request);
                }
                return;
            }

            organizationRequestService.markPaymentCompleted(
                    request.getId(),
                    "stripe-webhook",
                    paymentToken == null || paymentToken.isBlank() ? record.getReferenceToken() : paymentToken,
                    stripeSessionId
            );
            return;
        }

        if (!paid) {
            return;
        }

        Integer organizationId = record.getOrganizationId();
        if (organizationId == null) {
            return;
        }

        if (record.getFlowType() == StripeCheckoutFlow.SUBSCRIPTION) {
            organizationBillingService.markSubscriptionPaymentSuccess(
                    organizationId,
                    metadataPlanCode == null || metadataPlanCode.isBlank() ? record.getPlanCode() : metadataPlanCode,
                    metadataBillingCycle == null || metadataBillingCycle.isBlank()
                            ? record.getBillingCycle()
                            : SubscriptionBillingCycle.valueOf(metadataBillingCycle.trim().toUpperCase(Locale.ROOT)),
                    metadataSubscriptionAction == null || metadataSubscriptionAction.isBlank()
                            ? record.getSubscriptionAction()
                            : metadataSubscriptionAction,
                    stripeSessionId,
                    paymentIntentId,
                    customerEmail,
                    customerName
            );
            return;
        }

        if (record.getFlowType() == StripeCheckoutFlow.MODULE_PURCHASE) {
            organizationBillingService.markModulePurchasePaymentSuccess(
                    organizationId,
                    metadataModuleCode == null || metadataModuleCode.isBlank() ? record.getModuleCode() : metadataModuleCode,
                    stripeSessionId,
                    paymentIntentId,
                    customerEmail,
                    customerName
            );
        }
    }

    private String extractMetadataValue(Map<String, String> metadata, String fieldName) {
        if (metadata == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }

        String value = metadata.get(fieldName);
        return value == null || value.isBlank() ? null : value;
    }

    private String extractText(JsonNode node, String fieldName) {
        if (node == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        String value = node.path(fieldName).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private String buildRedirectUrl(String baseUrl, Map<String, String> sessionContext, boolean success) {
        String url = normalize(baseUrl);
        if (success) {
            url = appendQueryParameter(url, "session_id", "{CHECKOUT_SESSION_ID}");
        }
        for (Map.Entry<String, String> entry : sessionContext.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                url = appendQueryParameter(url, entry.getKey(), entry.getValue());
            }
        }
        return url;
    }

    private Map<String, String> sessionContext(
            String paymentToken,
            Integer requestId,
            Integer organizationId,
            String planCode,
            String moduleCode,
            SubscriptionBillingCycle billingCycle,
            String subscriptionAction,
            String slug,
            String name
    ) {
        return Map.of(
                "flow", paymentToken != null
                        ? StripeCheckoutFlow.ORGANIZATION_REQUEST.name()
                        : moduleCode != null && !moduleCode.isBlank()
                        ? StripeCheckoutFlow.MODULE_PURCHASE.name()
                        : StripeCheckoutFlow.SUBSCRIPTION.name(),
                "paymentToken", safe(paymentToken),
                "organizationRequestId", requestId == null ? "" : String.valueOf(requestId),
                "organizationId", organizationId == null ? "" : String.valueOf(organizationId),
                "planCode", safe(planCode),
                "moduleCode", safe(moduleCode),
                "billingCycle", billingCycle == null ? "" : billingCycle.name(),
                "subscriptionAction", safe(subscriptionAction),
                "organizationSlug", safe(slug),
                "organizationName", safe(name)
        );
    }

    private String appendQueryParameter(String url, String key, String value) {
        if (value == null || value.isBlank()) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        String encodedValue = "{CHECKOUT_SESSION_ID}".equals(value)
                ? value
                : URLEncoder.encode(value, StandardCharsets.UTF_8);
        return url + separator + key + "=" + encodedValue;
    }

    private long toStripeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required for Stripe checkout");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private StripeCheckoutSessionDto toDto(StripeCheckoutSessionRecord record) {
        if (record == null) {
            return null;
        }

        return StripeCheckoutSessionDto.builder()
                .id(record.getId())
                .stripeSessionId(record.getStripeSessionId())
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
                .checkoutUrl(record.getCheckoutUrl())
                .stripePaymentIntentId(record.getStripePaymentIntentId())
                .description(record.getDescription())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .completedAt(record.getCompletedAt())
                .build();
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
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

    public record PreparedCheckout(
            String organizationName,
            String organizationSlug,
            String customerEmail,
            String customerName,
            BigDecimal amount,
            String productName,
            String description,
            String clientReferenceId,
            Integer organizationRequestId,
            Integer organizationId,
            String planCode,
            String moduleCode,
            SubscriptionBillingCycle billingCycle,
            String subscriptionAction,
            String moduleSummary,
            String referenceToken,
            Map<String, String> sessionContext
    ) {
    }
}
