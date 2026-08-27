package tn.esprit.tic.civiAgora.service;

import org.junit.jupiter.api.Test;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationSubscriptionAccessPolicyTest {

    private final OrganizationSubscriptionAccessPolicy policy = new OrganizationSubscriptionAccessPolicy();

    @Test
    void legacyActiveOrganizationWithoutSubscriptionMetadataHasAccess() {
        Organization organization = organization(OrganizationStatus.ACTIVE, null, null);

        assertTrue(policy.hasActiveAccess(organization));
    }

    @Test
    void explicitActiveSubscriptionWithFutureEndHasAccess() {
        Organization organization = organization(
                OrganizationStatus.ACTIVE,
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now().plusDays(30)
        );

        assertTrue(policy.hasActiveAccess(organization));
    }

    @Test
    void nonOperationalOrganizationIsBlockedEvenWithActiveSubscription() {
        Organization organization = organization(
                OrganizationStatus.INACTIVE,
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now().plusDays(30)
        );

        assertFalse(policy.hasActiveAccess(organization));
    }

    @Test
    void suspendedCanceledAndExplicitlyExpiredSubscriptionsAreBlocked() {
        assertFalse(policy.hasActiveAccess(organization(
                OrganizationStatus.ACTIVE, SubscriptionStatus.SUSPENDED, LocalDateTime.now().plusDays(30)
        )));
        assertFalse(policy.hasActiveAccess(organization(
                OrganizationStatus.ACTIVE, SubscriptionStatus.CANCELED, LocalDateTime.now().plusDays(30)
        )));
        assertFalse(policy.hasActiveAccess(organization(
                OrganizationStatus.ACTIVE, SubscriptionStatus.EXPIRED, LocalDateTime.now().plusDays(30)
        )));
    }

    @Test
    void endedActiveSubscriptionIsBlocked() {
        Organization organization = organization(
                OrganizationStatus.ACTIVE,
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now().minusSeconds(1)
        );

        assertFalse(policy.hasActiveAccess(organization));
    }

    private Organization organization(
            OrganizationStatus organizationStatus,
            SubscriptionStatus subscriptionStatus,
            LocalDateTime subscriptionEndAt
    ) {
        Organization organization = new Organization();
        organization.setStatus(organizationStatus);
        organization.setSubscriptionStatus(subscriptionStatus);
        organization.setSubscriptionEndAt(subscriptionEndAt);
        return organization;
    }
}
