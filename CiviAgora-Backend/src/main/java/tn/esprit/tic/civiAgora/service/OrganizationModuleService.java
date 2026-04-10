package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.mappers.moduleMappers.OrganizationModuleMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationModuleService {

    private final OrganizationModuleRepository organizationModuleRepository;
    private final OrganizationRepository organizationRepository;
    private final ModuleService moduleService;
    private final OrganizationModuleMapper organizationModuleMapper;
    private final TenantAccessService tenantAccessService;

    public List<OrganizationModuleDto> getAllModulesForOrganization(Integer organizationId) {
        return organizationModuleRepository.findByOrganizationId(organizationId)
                .stream()
                .map(organizationModuleMapper::toDto)
                .toList();
    }

    public List<OrganizationModuleDto> getVisibleModulesForOrganization(Integer organizationId) {
        return organizationModuleRepository
                .findByOrganizationIdAndGrantedBySaasTrueAndEnabledByOrganizationTrueOrderByDisplayOrderAsc(organizationId)
                .stream()
                .map(organizationModuleMapper::toDto)
                .toList();
    }

    public List<OrganizationModuleDto> getVisibleModulesForCurrentOrganization() {
        Organization organization = tenantAccessService.getResolvedOrganizationOrThrow();
        return getVisibleModulesForOrganization(organization.getId());
    }

    public List<OrganizationModuleDto> getTenantModules(Integer organizationId) {
        tenantAccessService.assertOrganizationAccessOrThrow(organizationId);
        return getAllModulesForOrganization(organizationId);
    }

    public OrganizationModuleDto grantModuleToOrganization(Integer organizationId, String moduleCode, Integer displayOrder) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        Module module = moduleService.getModuleByCode(moduleCode);

        OrganizationModule saved = organizationModuleRepository
                .findByOrganizationIdAndModuleCode(organizationId, moduleCode)
                .map(existing -> {
                    existing.setGrantedBySaas(true);
                    existing.setEnabledByOrganization(true);
                    existing.setDisplayOrder(displayOrder);
                    return organizationModuleRepository.save(existing);
                })
                .orElseGet(() -> {
                    OrganizationModule orgModule = OrganizationModule.builder()
                            .organization(organization)
                            .module(module)
                            .grantedBySaas(true)
                            .enabledByOrganization(true)
                            .displayOrder(displayOrder)
                            .build();
                    return organizationModuleRepository.save(orgModule);
                });

        return organizationModuleMapper.toDto(saved);
    }

    public OrganizationModuleDto updateModuleVisibilityForOrganization(Integer organizationId, String moduleCode, Boolean enabledByOrganization) {
        OrganizationModule organizationModule = organizationModuleRepository
                .findByOrganizationIdAndModuleCode(organizationId, moduleCode)
                .orElseThrow(() -> new RuntimeException("Granted module not found for organization"));

        if (!Boolean.TRUE.equals(organizationModule.getGrantedBySaas())) {
            throw new RuntimeException("Module is not granted to this organization");
        }

        organizationModule.setEnabledByOrganization(enabledByOrganization);
        return organizationModuleMapper.toDto(organizationModuleRepository.save(organizationModule));
    }

    public OrganizationModuleDto updateTenantModuleVisibilityForOrganization(Integer organizationId, String moduleCode, Boolean enabledByOrganization) {
        tenantAccessService.assertOrganizationAccessOrThrow(organizationId);
        return updateModuleVisibilityForOrganization(organizationId, moduleCode, enabledByOrganization);
    }

    public void removeGrantedModule(Integer organizationId, String moduleCode) {
        OrganizationModule organizationModule = organizationModuleRepository
                .findByOrganizationIdAndModuleCode(organizationId, moduleCode)
                .orElseThrow(() -> new RuntimeException("Granted module not found for organization"));

        organizationModule.setGrantedBySaas(false);
        organizationModule.setEnabledByOrganization(false);
        organizationModuleRepository.save(organizationModule);
    }
}
