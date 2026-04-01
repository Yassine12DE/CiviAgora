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
    Optional<OrganizationRequest> findByDesiredSlug(String desiredSlug);
}
