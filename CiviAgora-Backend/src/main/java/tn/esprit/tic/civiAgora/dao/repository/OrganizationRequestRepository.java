package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRequestRepository extends JpaRepository<OrganizationRequest, Integer> {
    List<OrganizationRequest> findByRequestStatus(OrganizationRequestStatus status);
    List<OrganizationRequest> findAllByOrderByCreatedAtDesc();
    List<OrganizationRequest> findByRequestStatusOrderByCreatedAtDesc(OrganizationRequestStatus status);
    Optional<OrganizationRequest> findByDesiredSlug(String desiredSlug);
    Optional<OrganizationRequest> findByPaymentTokenHash(String paymentTokenHash);
    boolean existsByDesiredSlugIgnoreCase(String desiredSlug);
    boolean existsByDesiredSlugIgnoreCaseAndRequestStatusIn(String desiredSlug, List<OrganizationRequestStatus> statuses);
    boolean existsByContactEmailIgnoreCaseAndRequestStatusIn(String contactEmail, List<OrganizationRequestStatus> statuses);
    boolean existsByAdminEmailIgnoreCaseAndRequestStatusIn(String adminEmail, List<OrganizationRequestStatus> statuses);
}
