package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.tic.civiAgora.dao.entity.ModulePurchase;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModulePurchaseStatus;

import java.util.List;
import java.util.Optional;

public interface ModulePurchaseRepository extends JpaRepository<ModulePurchase, Long> {
    List<ModulePurchase> findByOrganizationId(Integer organizationId);
    List<ModulePurchase> findByStatus(ModulePurchaseStatus status);
    Optional<ModulePurchase> findByStripeSessionId(String stripeSessionId);
    Optional<ModulePurchase> findByOrganizationIdAndModuleCode(Integer organizationId, String moduleCode);
}
