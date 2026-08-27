package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.tic.civiAgora.dao.entity.StripeCheckoutSessionRecord;

import java.util.List;
import java.util.Optional;

@Repository
public interface StripeCheckoutSessionRepository extends JpaRepository<StripeCheckoutSessionRecord, Integer> {
    Optional<StripeCheckoutSessionRecord> findByStripeSessionId(String stripeSessionId);
    Optional<StripeCheckoutSessionRecord> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<StripeCheckoutSessionRecord> findByOrganizationIdOrderByCreatedAtDesc(Integer organizationId);
}
