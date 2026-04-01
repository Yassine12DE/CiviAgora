package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.tic.civiAgora.dao.entity.ModuleRequest;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleRequestStatus;

import java.util.List;

public interface ModuleRequestRepository extends JpaRepository<ModuleRequest, Long> {
    List<ModuleRequest> findByOrganizationId(Integer organizationId);
    List<ModuleRequest> findByStatus(ModuleRequestStatus status);
    boolean existsByOrganizationIdAndModuleCodeAndStatus(Integer organizationId, String code, ModuleRequestStatus status);
}