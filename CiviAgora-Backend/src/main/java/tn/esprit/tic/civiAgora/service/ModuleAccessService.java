package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModuleAccessService {

    private final OrganizationModuleRepository organizationModuleRepository;
    private final TenantAccessService tenantAccessService;

    public List<Map<String, Object>> getModulesForCurrentUser() {
        Organization currentOrganization = tenantAccessService.getCurrentOrganizationEntityOrThrow();

        List<OrganizationModule> modules =
                organizationModuleRepository
                        .findByOrganizationIdAndGrantedBySaasTrueAndEnabledByOrganizationTrueOrderByDisplayOrderAsc(
                                currentOrganization.getId()
                        );

        return modules.stream()
                .map(om -> Map.<String, Object>of(
                        "id", om.getModule().getId(),
                        "code", om.getModule().getCode(),
                        "name", om.getModule().getName(),
                        "description", om.getModule().getDescription() == null ? "" : om.getModule().getDescription(),
                        "active", om.getModule().getActive() == null ? true : om.getModule().getActive(),
                        "displayOrder", om.getDisplayOrder() == null ? 0 : om.getDisplayOrder()
                ))
                .toList();
    }
}
