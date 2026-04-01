package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;

import java.util.List;
import java.util.Optional;

public interface OrganizationModuleRepository extends JpaRepository<OrganizationModule, Long> {
    List<OrganizationModule> findByOrganization(Organization organization);
    List<OrganizationModule> findByOrganizationId(Integer organizationId);
    List<OrganizationModule> findByOrganizationIdAndGrantedBySaasTrue(Integer organizationId);
    List<OrganizationModule> findByOrganizationIdAndGrantedBySaasTrueAndEnabledByOrganizationTrueOrderByDisplayOrderAsc(Integer organizationId);
    Optional<OrganizationModule> findByOrganizationIdAndModuleCode(Integer organizationId, String code);
}