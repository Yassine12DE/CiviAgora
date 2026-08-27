package tn.esprit.tic.civiAgora.service;

import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionStatus;

import java.time.LocalDateTime;

@Component
public class OrganizationSubscriptionAccessPolicy {

    public boolean hasActiveAccess(Organization organization) {
        return resolveStatus(organization) == SubscriptionStatus.ACTIVE;
    }

    public SubscriptionStatus resolveStatus(Organization organization) {
        if (organization == null) {
            return SubscriptionStatus.PENDING_PAYMENT;
        }

        SubscriptionStatus explicitStatus = organization.getSubscriptionStatus();

        if (explicitStatus == SubscriptionStatus.CANCELED
                || explicitStatus == SubscriptionStatus.SUSPENDED) {
            return explicitStatus;
        }

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            return SubscriptionStatus.EXPIRED;
        }

        if (explicitStatus == SubscriptionStatus.EXPIRED
                || explicitStatus == SubscriptionStatus.PENDING_PAYMENT) {
            return explicitStatus;
        }

        LocalDateTime subscriptionEndAt = organization.getSubscriptionEndAt();
        if (subscriptionEndAt != null && !subscriptionEndAt.isAfter(LocalDateTime.now())) {
            return SubscriptionStatus.EXPIRED;
        }

        if (explicitStatus == SubscriptionStatus.ACTIVE) {
            return SubscriptionStatus.ACTIVE;
        }

        if (hasNoSubscriptionMetadata(organization)) {
            return SubscriptionStatus.ACTIVE;
        }

        if (subscriptionEndAt != null) {
            return SubscriptionStatus.ACTIVE;
        }

        return SubscriptionStatus.PENDING_PAYMENT;
    }

    private boolean hasNoSubscriptionMetadata(Organization organization) {
        return isBlank(organization.getSubscriptionPlanCode())
                && organization.getSubscriptionBillingCycle() == null
                && organization.getSubscriptionStatus() == null
                && organization.getSubscriptionStartAt() == null
                && organization.getSubscriptionEndAt() == null
                && organization.getSubscriptionLastRenewedAt() == null
                && organization.getSubscriptionPendingSince() == null
                && organization.getSubscriptionAutoRenew() == null
                && organization.getSubscriptionRenewalCount() == null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
