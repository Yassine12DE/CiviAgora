package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationSettings;

import java.util.Optional;

public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, Long> {
    Optional<OrganizationSettings> findByOrganizationId(Integer organizationId);
    Optional<OrganizationSettings> findByIdAndOrganizationId(Long id, Integer organizationId);
}
