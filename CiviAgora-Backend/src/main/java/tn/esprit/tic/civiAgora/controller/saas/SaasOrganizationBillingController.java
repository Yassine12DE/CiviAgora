package tn.esprit.tic.civiAgora.controller.saas;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dto.billingDto.OrganizationBillingOverviewDto;
import tn.esprit.tic.civiAgora.dto.billingDto.OrganizationSubscriptionDto;
import tn.esprit.tic.civiAgora.service.OrganizationBillingService;

@RestController
@RequestMapping("/saas/organizations/{organizationId}")
@RequiredArgsConstructor
public class SaasOrganizationBillingController {

    private final OrganizationBillingService organizationBillingService;

    @GetMapping("/subscription")
    public ResponseEntity<OrganizationSubscriptionDto> getSubscription(
            @PathVariable("organizationId") Integer organizationId
    ) {
        return ResponseEntity.ok(organizationBillingService.getSubscription(organizationId));
    }

    @GetMapping("/billing")
    public ResponseEntity<OrganizationBillingOverviewDto> getBillingOverview(
            @PathVariable("organizationId") Integer organizationId
    ) {
        return ResponseEntity.ok(organizationBillingService.getBillingOverview(organizationId));
    }

    @PatchMapping("/subscription/status")
    public ResponseEntity<OrganizationSubscriptionDto> updateSubscriptionStatus(
            @PathVariable("organizationId") Integer organizationId,
            @RequestParam("status") String status
    ) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        return ResponseEntity.ok(switch (normalized) {
            case "ACTIVE" -> organizationBillingService.extendSubscription(organizationId, 1);
            case "SUSPENDED" -> organizationBillingService.suspendSubscription(organizationId);
            case "CANCELED" -> organizationBillingService.cancelSubscription(organizationId);
            default -> throw new IllegalArgumentException("Unsupported subscription status: " + status);
        });
    }

    @PatchMapping("/subscription/extend")
    public ResponseEntity<OrganizationSubscriptionDto> extendSubscription(
            @PathVariable("organizationId") Integer organizationId,
            @RequestParam(value = "months", defaultValue = "1") int months
    ) {
        return ResponseEntity.ok(organizationBillingService.extendSubscription(organizationId, months));
    }
}
