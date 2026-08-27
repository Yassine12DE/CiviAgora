package tn.esprit.tic.civiAgora.controller.back_office;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutFlow;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionBillingCycle;
import tn.esprit.tic.civiAgora.dto.billingDto.OrganizationBillingOverviewDto;
import tn.esprit.tic.civiAgora.dto.billingDto.OrganizationSubscriptionDto;
import tn.esprit.tic.civiAgora.dto.moduleDto.ModuleDto;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.dto.moduleRequestDto.ModuleRequestDto;
import tn.esprit.tic.civiAgora.dto.organizationSettingsDto.OrganizationSettingsDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionCreateRequestDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripePaymentIntentDto;
import tn.esprit.tic.civiAgora.mappers.moduleMappers.ModuleMapper;
import tn.esprit.tic.civiAgora.service.ModuleService;
import tn.esprit.tic.civiAgora.service.ModuleRequestService;
import tn.esprit.tic.civiAgora.service.OrganizationBillingService;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;
import tn.esprit.tic.civiAgora.service.OrganizationSettingsService;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.service.StripeCheckoutService;
import tn.esprit.tic.civiAgora.service.StripePaymentIntentService;

import java.util.List;

@RestController
@RequestMapping("/org/{organizationId}")
@RequiredArgsConstructor
public class OrganizationBackOfficeController {

    private final OrganizationModuleService organizationModuleService;
    private final OrganizationSettingsService organizationSettingsService;
    private final ModuleRequestService moduleRequestService;
    private final OrganizationBillingService organizationBillingService;
    private final RbacService rbacService;
    private final ModuleService moduleService;
    private final ModuleMapper moduleMapper;
    private final StripeCheckoutService stripeCheckoutService;
    private final StripePaymentIntentService stripePaymentIntentService;

    @GetMapping("/modules")
    public ResponseEntity<List<OrganizationModuleDto>> getGrantedModules(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantBackOfficeAccess(organizationId);
        return ResponseEntity.ok(organizationModuleService.getTenantModules(organizationId));
    }

    @PatchMapping("/modules/{moduleCode}/visibility")
    public ResponseEntity<OrganizationModuleDto> updateModuleVisibility(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("moduleCode") String moduleCode,
            @RequestParam("enabled") Boolean enabled
    ) {
        rbacService.requireTenantModuleVisibilityAccess(organizationId);
        return ResponseEntity.ok(
                organizationModuleService.updateTenantModuleVisibilityForOrganization(
                        organizationId,
                        moduleCode,
                        enabled
                )
        );
    }

    @GetMapping("/settings")
    public ResponseEntity<OrganizationSettingsDto> getSettings(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantDesignCustomizationAccess(organizationId);
        return ResponseEntity.ok(organizationSettingsService.getTenantSettings(organizationId));
    }

    @PutMapping("/settings")
    public ResponseEntity<OrganizationSettingsDto> updateSettings(
            @PathVariable("organizationId") Integer organizationId,
            @RequestBody OrganizationSettingsDto settings
    ) {
        rbacService.requireTenantDesignCustomizationAccess(organizationId);
        return ResponseEntity.ok(organizationSettingsService.updateTenantSettings(organizationId, settings));
    }

    @PostMapping("/module-requests/{moduleCode}")
    public ResponseEntity<ModuleRequestDto> createModuleRequest(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("moduleCode") String moduleCode,
            @RequestParam(value = "comment", required = false) String comment
    ) {
        rbacService.requireTenantModuleRequestAccess(organizationId);
        return ResponseEntity.ok(
                moduleRequestService.createTenantRequest(organizationId, moduleCode, comment)
        );
    }

    @GetMapping("/module-requests")
    public ResponseEntity<List<ModuleRequestDto>> getOrganizationRequests(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantModuleRequestAccess(organizationId);
        return ResponseEntity.ok(moduleRequestService.getTenantRequestsByOrganization(organizationId));
    }

    @GetMapping("/module-catalog")
    public ResponseEntity<List<ModuleDto>> getTenantModuleCatalog(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantModuleRequestAccess(organizationId);
        return ResponseEntity.ok(
                moduleService.getTenantRequestableModules()
                        .stream()
                        .map(moduleMapper::toDto)
                        .toList()
        );
    }

    @GetMapping("/subscription")
    public ResponseEntity<OrganizationSubscriptionDto> getSubscription(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantBackOfficeAccess(organizationId);
        return ResponseEntity.ok(organizationBillingService.getSubscription(organizationId));
    }

    @GetMapping("/billing")
    public ResponseEntity<OrganizationBillingOverviewDto> getBillingOverview(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantBackOfficeAccess(organizationId);
        return ResponseEntity.ok(organizationBillingService.getBillingOverview(organizationId));
    }

    @PostMapping("/subscription/extend")
    public ResponseEntity<OrganizationSubscriptionDto> extendSubscription(
            @PathVariable("organizationId") Integer organizationId,
            @RequestParam(value = "months", defaultValue = "1") int months
    ) {
        rbacService.requireTenantBackOfficeAccess(organizationId);
        return ResponseEntity.ok(organizationBillingService.extendSubscription(organizationId, months));
    }

    @PostMapping("/subscription/suspend")
    public ResponseEntity<OrganizationSubscriptionDto> suspendSubscription(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantBackOfficeAccess(organizationId);
        return ResponseEntity.ok(organizationBillingService.suspendSubscription(organizationId));
    }

    @PostMapping("/subscription/cancel")
    public ResponseEntity<OrganizationSubscriptionDto> cancelSubscription(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantBackOfficeAccess(organizationId);
        return ResponseEntity.ok(organizationBillingService.cancelSubscription(organizationId));
    }

    @PostMapping("/module-purchases/{moduleCode}/checkout")
    public ResponseEntity<StripeCheckoutSessionDto> createModulePurchaseCheckout(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("moduleCode") String moduleCode,
            @RequestParam(value = "billingCycle", required = false) SubscriptionBillingCycle billingCycle,
            @RequestParam(value = "comment", required = false) String comment
    ) {
        rbacService.requireTenantModuleRequestAccess(organizationId);
        StripeCheckoutSessionCreateRequestDto request = StripeCheckoutSessionCreateRequestDto.builder()
                .flowType(StripeCheckoutFlow.MODULE_PURCHASE)
                .organizationId(organizationId)
                .moduleCode(moduleCode)
                .billingCycle(billingCycle)
                .customerEmail(null)
                .customerName(null)
                .planCode(null)
                .subscriptionAction("PURCHASE")
                .build();
        return ResponseEntity.ok(stripeCheckoutService.createCheckoutSession(request));
    }

    @PostMapping("/module-purchases/{moduleCode}/payment-intents")
    public ResponseEntity<StripePaymentIntentDto> createModulePurchasePaymentIntent(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("moduleCode") String moduleCode,
            @RequestParam(value = "billingCycle", required = false) SubscriptionBillingCycle billingCycle,
            @RequestParam(value = "comment", required = false) String comment
    ) {
        rbacService.requireTenantModuleRequestAccess(organizationId);
        StripeCheckoutSessionCreateRequestDto request = StripeCheckoutSessionCreateRequestDto.builder()
                .flowType(StripeCheckoutFlow.MODULE_PURCHASE)
                .organizationId(organizationId)
                .moduleCode(moduleCode)
                .billingCycle(billingCycle)
                .customerEmail(null)
                .customerName(null)
                .planCode(null)
                .subscriptionAction("PURCHASE")
                .build();
        return ResponseEntity.ok(stripePaymentIntentService.createPaymentIntent(request));
    }

    @PostMapping("/stripe/payment-intents/{paymentIntentId}/sync")
    public ResponseEntity<StripePaymentIntentDto> syncTenantPaymentIntent(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("paymentIntentId") String paymentIntentId
    ) {
        rbacService.requireTenantBackOfficeAccess(organizationId);
        return ResponseEntity.ok(stripePaymentIntentService.syncPaymentIntent(paymentIntentId));
    }
}
