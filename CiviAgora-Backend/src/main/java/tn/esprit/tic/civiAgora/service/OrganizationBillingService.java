package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.ModulePurchase;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.entity.StripeCheckoutSessionRecord;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleBillingType;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleScope;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModulePurchaseStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutFlow;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionBillingCycle;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionStatus;
import tn.esprit.tic.civiAgora.dao.repository.ModulePurchaseRepository;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRequestRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dao.repository.StripeCheckoutSessionRepository;
import tn.esprit.tic.civiAgora.dto.billingDto.ModulePurchaseDto;
import tn.esprit.tic.civiAgora.dto.billingDto.OrganizationBillingOverviewDto;
import tn.esprit.tic.civiAgora.dto.billingDto.OrganizationSubscriptionDto;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.dto.moduleRequestDto.ModuleRequestDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionDto;
import tn.esprit.tic.civiAgora.mappers.moduleMappers.OrganizationModuleMapper;
import tn.esprit.tic.civiAgora.mappers.moduleRequestMappers.ModuleRequestMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrganizationBillingService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationModuleRepository organizationModuleRepository;
    private final ModulePurchaseRepository modulePurchaseRepository;
    private final ModuleRequestRepository moduleRequestRepository;
    private final StripeCheckoutSessionRepository stripeCheckoutSessionRepository;
    private final OrganizationModuleMapper organizationModuleMapper;
    private final ModuleRequestMapper moduleRequestMapper;
    private final ModuleService moduleService;
    private final ModuleNotificationEmailService moduleNotificationEmailService;
    private final TenantAccessService tenantAccessService;
    private final BillingPricingService billingPricingService;
    private final OrganizationSubscriptionAccessPolicy subscriptionAccessPolicy;

    @Transactional(readOnly = true)
    public OrganizationSubscriptionDto getSubscription(Integer organizationId) {
        Organization organization = ensureOrganization(organizationId);
        syncSubscriptionStatus(organization);
        return toSubscriptionDto(organization);
    }

    @Transactional(readOnly = true)
    public OrganizationBillingOverviewDto getBillingOverview(Integer organizationId) {
        Organization organization = ensureOrganization(organizationId);
        syncSubscriptionStatus(organization);

        List<OrganizationModuleDto> activeModules = getTenantModulesForOrganization(organizationId);
        List<ModuleRequestDto> pendingRequests = moduleRequestRepository.findByOrganizationId(organizationId).stream()
                .map(moduleRequestMapper::toDto)
                .filter(request -> "PENDING".equalsIgnoreCase(request.getStatus())
                        || "PENDING_PAYMENT".equalsIgnoreCase(request.getStatus()))
                .toList();
        List<ModulePurchaseDto> modulePurchases = modulePurchaseRepository.findByOrganizationId(organizationId).stream()
                .map(this::toModulePurchaseDto)
                .sorted(Comparator.comparing(ModulePurchaseDto::getRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        List<StripeCheckoutSessionDto> recentPayments = stripeCheckoutSessionRepository
                .findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(this::toStripeDto)
                .limit(10)
                .toList();

        return OrganizationBillingOverviewDto.builder()
                .subscription(toSubscriptionDto(organization))
                .activeModules(activeModules)
                .pendingModuleRequests(pendingRequests)
                .modulePurchases(modulePurchases)
                .recentPayments(recentPayments)
                .build();
    }

    @Transactional
    public ModulePurchase createModulePurchaseRequest(Integer organizationId, String moduleCode, String comment) {
        Organization organization = ensureOrganization(organizationId);
        syncSubscriptionStatus(organization);
        if (!isSubscriptionActive(organization)) {
            throw new IllegalStateException("An active subscription is required before purchasing additional modules");
        }

        Module module = moduleService.getModuleByCode(moduleCode);
        ModulePurchase purchase = modulePurchaseRepository
                .findByOrganizationIdAndModuleCode(organizationId, moduleCode)
                .orElseGet(ModulePurchase::new);

        purchase.setOrganization(organization);
        purchase.setModule(module);
        purchase.setStatus(ModulePurchaseStatus.PENDING_PAYMENT);
        purchase.setBillingType(resolveBillingType(module));
        purchase.setAmount(resolveModuleAmount(module, purchase.getBillingType()));
        purchase.setCurrency("usd");
        purchase.setCustomerEmail(organization.getEmail());
        purchase.setCustomerName(organization.getName());
        purchase.setComment(comment);
        purchase.setUpdatedAt(LocalDateTime.now());
        if (purchase.getRequestedAt() == null) {
            purchase.setRequestedAt(LocalDateTime.now());
        }

        return modulePurchaseRepository.save(purchase);
    }

    @Transactional
    public void markSubscriptionPaymentSuccess(
            Integer organizationId,
            String planCode,
            SubscriptionBillingCycle billingCycle,
            String subscriptionAction,
            String stripeSessionId,
            String paymentIntentId,
            String customerEmail,
            String customerName
    ) {
        Organization organization = ensureOrganization(organizationId);
        LocalDateTime now = LocalDateTime.now();
        SubscriptionBillingCycle resolvedCycle = billingCycle == null
                ? SubscriptionBillingCycle.MONTHLY
                : billingCycle;
        int months = billingPricingService.resolveSubscriptionMonths(resolvedCycle);

        LocalDateTime baseDate = organization.getSubscriptionEndAt() != null && organization.getSubscriptionEndAt().isAfter(now)
                ? organization.getSubscriptionEndAt()
                : now;
        LocalDateTime newEndAt = baseDate.plusMonths(months);
        if (organization.getSubscriptionStartAt() == null || "NEW".equalsIgnoreCase(subscriptionAction)) {
            organization.setSubscriptionStartAt(now);
        }

        organization.setSubscriptionPlanCode(planCode == null || planCode.isBlank()
                ? organization.getSubscriptionPlanCode()
                : planCode.trim().toUpperCase());
        organization.setSubscriptionBillingCycle(resolvedCycle);
        organization.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        organization.setSubscriptionEndAt(newEndAt);
        organization.setSubscriptionLastRenewedAt(now);
        organization.setSubscriptionPendingSince(null);
        organization.setSubscriptionAutoRenew(Boolean.TRUE.equals(organization.getSubscriptionAutoRenew()));
        organization.setSubscriptionRenewalCount(resolveRenewalCount(organization) + 1);
        organizationRepository.save(organization);

        StripeCheckoutSessionRecord sessionRecord = stripeCheckoutSessionRepository
                .findByStripeSessionId(stripeSessionId)
                .orElse(null);
        if (sessionRecord != null) {
            sessionRecord.setPaymentStatus(StripeCheckoutStatus.COMPLETED);
            sessionRecord.setCompletedAt(now);
            sessionRecord.setUpdatedAt(now);
            sessionRecord.setStripePaymentIntentId(paymentIntentId);
            sessionRecord.setCustomerEmail(customerEmail);
            sessionRecord.setCustomerName(customerName);
            stripeCheckoutSessionRepository.save(sessionRecord);
        }
    }

    @Transactional
    public void markSubscriptionPaymentFailed(Integer organizationId, String stripeSessionId) {
        Organization organization = ensureOrganization(organizationId);
        organization.setSubscriptionStatus(SubscriptionStatus.PENDING_PAYMENT);
        organization.setSubscriptionPendingSince(LocalDateTime.now());
        organizationRepository.save(organization);

        StripeCheckoutSessionRecord sessionRecord = stripeCheckoutSessionRepository
                .findByStripeSessionId(stripeSessionId)
                .orElse(null);
        if (sessionRecord != null) {
            sessionRecord.setPaymentStatus(StripeCheckoutStatus.FAILED);
            sessionRecord.setUpdatedAt(LocalDateTime.now());
            stripeCheckoutSessionRepository.save(sessionRecord);
        }
    }

    @Transactional
    public void markModulePurchasePaymentSuccess(
            Integer organizationId,
            String moduleCode,
            String stripeSessionId,
            String paymentIntentId,
            String customerEmail,
            String customerName
    ) {
        Organization organization = ensureOrganization(organizationId);
        Module module = moduleService.getModuleByCode(moduleCode);
        LocalDateTime now = LocalDateTime.now();

        ModulePurchase purchase = modulePurchaseRepository.findByOrganizationIdAndModuleCode(organizationId, moduleCode)
                .orElseThrow(() -> new NoSuchElementException("Module purchase not found for " + moduleCode));
        purchase.setStatus(ModulePurchaseStatus.ACTIVE);
        purchase.setStripeSessionId(stripeSessionId);
        purchase.setStripePaymentIntentId(paymentIntentId);
        purchase.setCustomerEmail(customerEmail);
        purchase.setCustomerName(customerName);
        purchase.setPaidAt(now);
        purchase.setActivatedAt(now);
        purchase.setUpdatedAt(now);
        modulePurchaseRepository.save(purchase);

        grantModuleToOrganization(organization, module, null);

        StripeCheckoutSessionRecord sessionRecord = stripeCheckoutSessionRepository
                .findByStripeSessionId(stripeSessionId)
                .orElse(null);
        if (sessionRecord != null) {
            sessionRecord.setPaymentStatus(StripeCheckoutStatus.COMPLETED);
            sessionRecord.setCompletedAt(now);
            sessionRecord.setUpdatedAt(now);
            sessionRecord.setStripePaymentIntentId(paymentIntentId);
            sessionRecord.setCustomerEmail(customerEmail);
            sessionRecord.setCustomerName(customerName);
            stripeCheckoutSessionRepository.save(sessionRecord);
        }
    }

    @Transactional
    public void markModulePurchasePaymentFailed(Integer organizationId, String moduleCode, String stripeSessionId) {
        ModulePurchase purchase = modulePurchaseRepository.findByOrganizationIdAndModuleCode(organizationId, moduleCode)
                .orElse(null);
        if (purchase != null) {
            purchase.setStatus(ModulePurchaseStatus.FAILED);
            purchase.setUpdatedAt(LocalDateTime.now());
            modulePurchaseRepository.save(purchase);
        }

        StripeCheckoutSessionRecord sessionRecord = stripeCheckoutSessionRepository
                .findByStripeSessionId(stripeSessionId)
                .orElse(null);
        if (sessionRecord != null) {
            sessionRecord.setPaymentStatus(StripeCheckoutStatus.FAILED);
            sessionRecord.setUpdatedAt(LocalDateTime.now());
            stripeCheckoutSessionRepository.save(sessionRecord);
        }
    }

    @Transactional
    public OrganizationSubscriptionDto cancelSubscription(Integer organizationId) {
        Organization organization = ensureOrganization(organizationId);
        organization.setSubscriptionStatus(SubscriptionStatus.CANCELED);
        organization.setSubscriptionPendingSince(null);
        organizationRepository.save(organization);
        return toSubscriptionDto(organization);
    }

    @Transactional
    public OrganizationSubscriptionDto suspendSubscription(Integer organizationId) {
        Organization organization = ensureOrganization(organizationId);
        organization.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
        organization.setSubscriptionPendingSince(null);
        organizationRepository.save(organization);
        return toSubscriptionDto(organization);
    }

    @Transactional
    public OrganizationSubscriptionDto extendSubscription(Integer organizationId, int months) {
        Organization organization = ensureOrganization(organizationId);
        syncSubscriptionStatus(organization);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = organization.getSubscriptionEndAt() != null && organization.getSubscriptionEndAt().isAfter(now)
                ? organization.getSubscriptionEndAt()
                : now;
        organization.setSubscriptionEndAt(base.plusMonths(Math.max(months, 1)));
        organization.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        organization.setSubscriptionLastRenewedAt(now);
        organization.setSubscriptionRenewalCount(resolveRenewalCount(organization) + 1);
        organizationRepository.save(organization);
        return toSubscriptionDto(organization);
    }

    public boolean isSubscriptionActive(Integer organizationId) {
        Organization organization = ensureOrganization(organizationId);
        syncSubscriptionStatus(organization);
        return isSubscriptionActive(organization);
    }

    public boolean isSubscriptionActive(Organization organization) {
        return subscriptionAccessPolicy.hasActiveAccess(organization);
    }

    @Transactional
    public void syncSubscriptionStatus(Organization organization) {
        if (organization == null) {
            return;
        }

        SubscriptionStatus resolvedStatus = resolveStatus(organization);
        if (resolvedStatus != organization.getSubscriptionStatus()) {
            organization.setSubscriptionStatus(resolvedStatus);
            organizationRepository.save(organization);
        }
    }

    private SubscriptionStatus resolveStatus(Organization organization) {
        return subscriptionAccessPolicy.resolveStatus(organization);
    }

    private Organization ensureOrganization(Integer organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));
        return tenantAccessService.assertOrganizationAccessOrThrow(organization.getId());
    }

    private OrganizationSubscriptionDto toSubscriptionDto(Organization organization) {
        SubscriptionStatus status = resolveStatus(organization);
        LocalDateTime now = LocalDateTime.now();
        long remainingDays = organization.getSubscriptionEndAt() == null
                ? 0
                : Math.max(ChronoUnit.DAYS.between(now.toLocalDate(), organization.getSubscriptionEndAt().toLocalDate()), 0);
        long totalDays = organization.getSubscriptionStartAt() == null || organization.getSubscriptionEndAt() == null
                ? 0
                : Math.max(ChronoUnit.DAYS.between(organization.getSubscriptionStartAt().toLocalDate(), organization.getSubscriptionEndAt().toLocalDate()), 0);

        return OrganizationSubscriptionDto.builder()
                .organizationId(organization.getId())
                .organizationName(organization.getName())
                .organizationSlug(organization.getSlug())
                .planCode(organization.getSubscriptionPlanCode())
                .billingCycle(organization.getSubscriptionBillingCycle())
                .status(status)
                .startAt(organization.getSubscriptionStartAt())
                .endAt(organization.getSubscriptionEndAt())
                .lastRenewedAt(organization.getSubscriptionLastRenewedAt())
                .pendingSince(organization.getSubscriptionPendingSince())
                .autoRenew(organization.getSubscriptionAutoRenew())
                .renewalCount(resolveRenewalCount(organization))
                .remainingDays(remainingDays)
                .totalDays(totalDays)
                .expired(status == SubscriptionStatus.EXPIRED)
                .expiringSoon(status == SubscriptionStatus.ACTIVE && remainingDays <= 7)
                .build();
    }

    private int resolveRenewalCount(Organization organization) {
        return organization.getSubscriptionRenewalCount() == null ? 0 : organization.getSubscriptionRenewalCount();
    }

    private List<OrganizationModuleDto> getTenantModulesForOrganization(Integer organizationId) {
        return mapOrganizationModules(
                organizationModuleRepository.findByOrganizationIdAndGrantedBySaasTrue(organizationId)
                        .stream()
                        .filter(this::isModuleBackOfficeVisible)
                        .toList()
        );
    }

    private List<OrganizationModuleDto> mapOrganizationModules(List<OrganizationModule> organizationModules) {
        return organizationModules.stream()
                .filter(this::isModuleActive)
                .sorted(
                        Comparator
                                .comparing(
                                        OrganizationModule::getDisplayOrder,
                                        Comparator.nullsLast(Integer::compareTo)
                                )
                                .thenComparing(organizationModule -> organizationModule.getModule().getCode())
                )
                .map(organizationModuleMapper::toDto)
                .toList();
    }

    private boolean isModuleActive(OrganizationModule organizationModule) {
        return organizationModule != null
                && organizationModule.getModule() != null
                && Boolean.TRUE.equals(organizationModule.getModule().getActive());
    }

    private boolean isModuleBackOfficeVisible(OrganizationModule organizationModule) {
        if (!isModuleActive(organizationModule)) {
            return false;
        }
        return ModuleScope.resolveOrDefault(organizationModule.getModule().getScope()).allowsBackOffice();
    }

    private ModuleBillingType resolveBillingType(Module module) {
        if (module == null || module.getBillingType() == null) {
            return ModuleBillingType.ONE_TIME;
        }
        return module.getBillingType();
    }

    private BigDecimal resolveModuleAmount(Module module, ModuleBillingType billingType) {
        if (module == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal amount = switch (billingType == null ? ModuleBillingType.ONE_TIME : billingType) {
            case MONTHLY -> module.getMonthlyPrice();
            case YEARLY -> module.getYearlyPrice();
            case ONE_TIME -> module.getOneTimePrice();
        };
        if (amount == null || amount.signum() <= 0) {
            amount = Optional.ofNullable(module.getOneTimePrice())
                    .orElse(Optional.ofNullable(module.getMonthlyPrice()).orElse(module.getYearlyPrice()));
        }
        return amount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private OrganizationModuleDto toOrganizationModuleDto(OrganizationModule organizationModule) {
        return organizationModuleMapper.toDto(organizationModule);
    }

    private OrganizationModule grantModuleToOrganization(Organization organization, Module module, Integer displayOrder) {
        if (organization == null) {
            throw new IllegalStateException("Organization not found");
        }
        if (module == null || !Boolean.TRUE.equals(module.getActive())) {
            throw new IllegalStateException("Module is inactive in the global catalog");
        }

        OrganizationModule organizationModule = organizationModuleRepository
                .findByOrganizationIdAndModuleId(organization.getId(), module.getId())
                .orElseGet(() -> OrganizationModule.builder()
                        .organization(organization)
                        .module(module)
                        .build());

        boolean newlyGranted = !Boolean.TRUE.equals(organizationModule.getGrantedBySaas());

        organizationModule.setGrantedBySaas(true);
        organizationModule.setEnabledByOrganization(true);
        applyDisplayOrder(organizationModule, organization.getId(), displayOrder);
        OrganizationModule saved = organizationModuleRepository.save(organizationModule);
        if (newlyGranted) {
            moduleNotificationEmailService.sendModuleGrantedNotificationAfterCommit(
                    organization,
                    module.getName(),
                    module.getCode()
            );
        }
        return saved;
    }

    private void applyDisplayOrder(OrganizationModule organizationModule, Integer organizationId, Integer displayOrder) {
        if (displayOrder != null) {
            organizationModule.setDisplayOrder(displayOrder);
            return;
        }

        if (organizationModule.getDisplayOrder() != null) {
            return;
        }

        int nextDisplayOrder = organizationModuleRepository.findByOrganizationIdAndGrantedBySaasTrue(organizationId)
                .stream()
                .map(OrganizationModule::getDisplayOrder)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        organizationModule.setDisplayOrder(nextDisplayOrder);
    }

    private ModulePurchaseDto toModulePurchaseDto(ModulePurchase purchase) {
        return ModulePurchaseDto.builder()
                .id(purchase.getId())
                .organizationId(purchase.getOrganization() != null ? purchase.getOrganization().getId() : null)
                .organizationName(purchase.getOrganization() != null ? purchase.getOrganization().getName() : null)
                .moduleId(purchase.getModule() != null ? purchase.getModule().getId() : null)
                .moduleCode(purchase.getModule() != null ? purchase.getModule().getCode() : null)
                .moduleName(purchase.getModule() != null ? purchase.getModule().getName() : null)
                .billingType(purchase.getBillingType())
                .status(purchase.getStatus())
                .amount(purchase.getAmount())
                .currency(purchase.getCurrency())
                .stripeSessionId(purchase.getStripeSessionId())
                .stripePaymentIntentId(purchase.getStripePaymentIntentId())
                .customerEmail(purchase.getCustomerEmail())
                .customerName(purchase.getCustomerName())
                .comment(purchase.getComment())
                .requestedAt(purchase.getRequestedAt())
                .updatedAt(purchase.getUpdatedAt())
                .paidAt(purchase.getPaidAt())
                .activatedAt(purchase.getActivatedAt())
                .build();
    }

    private StripeCheckoutSessionDto toStripeDto(StripeCheckoutSessionRecord record) {
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
}
