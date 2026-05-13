package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.mappers.moduleMappers.OrganizationModuleMapper;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrganizationModuleService {

    private final OrganizationModuleRepository organizationModuleRepository;
    private final OrganizationRepository organizationRepository;
    private final ModuleService moduleService;
    private final OrganizationModuleMapper organizationModuleMapper;
    private final TenantAccessService tenantAccessService;

    public List<OrganizationModuleDto> getAllModulesForOrganization(Integer organizationId) {
        getOrganizationOrThrow(organizationId);
        return mapOrganizationModules(
                organizationModuleRepository.findByOrganizationIdAndGrantedBySaasTrue(organizationId)
        );
    }

    public List<OrganizationModuleDto> getVisibleModulesForOrganization(Integer organizationId) {
        getOrganizationOrThrow(organizationId);
        return mapOrganizationModules(
                organizationModuleRepository
                        .findByOrganizationIdAndGrantedBySaasTrueAndEnabledByOrganizationTrueOrderByDisplayOrderAsc(
                                organizationId
                        )
        );
    }

    public List<OrganizationModuleDto> getVisibleModulesForCurrentOrganization() {
        Organization organization = tenantAccessService.getResolvedOrganizationOrThrow();
        return getVisibleModulesForOrganization(organization.getId());
    }

    public List<OrganizationModuleDto> getTenantModules(Integer organizationId) {
        tenantAccessService.assertOrganizationAccessOrThrow(organizationId);
        return getAllModulesForOrganization(organizationId);
    }

    @Transactional
    public List<OrganizationModuleDto> addModuleToOrganization(
            Integer organizationId,
            String moduleReference,
            Integer displayOrder
    ) {
        Organization organization = getOrganizationOrThrow(organizationId);
        Module module = resolveModuleReference(moduleReference);

        OrganizationModule existing = organizationModuleRepository
                .findByOrganizationIdAndModuleId(organizationId, module.getId())
                .orElse(null);

        if (existing != null && Boolean.TRUE.equals(existing.getGrantedBySaas())) {
            throw new IllegalStateException("Module is already assigned to this organization");
        }

        OrganizationModule organizationModule = existing != null
                ? existing
                : OrganizationModule.builder()
                .organization(organization)
                .module(module)
                .build();

        organizationModule.setGrantedBySaas(true);
        organizationModule.setEnabledByOrganization(true);
        applyDisplayOrder(organizationModule, organizationId, displayOrder);
        organizationModuleRepository.save(organizationModule);

        return getAllModulesForOrganization(organizationId);
    }

    @Transactional
    public OrganizationModuleDto grantModuleToOrganization(Integer organizationId, String moduleCode, Integer displayOrder) {
        Organization organization = getOrganizationOrThrow(organizationId);
        Module module = moduleService.getModuleByCode(moduleCode);

        OrganizationModule organizationModule = organizationModuleRepository
                .findByOrganizationIdAndModuleId(organizationId, module.getId())
                .orElseGet(() -> OrganizationModule.builder()
                        .organization(organization)
                        .module(module)
                        .build());

        organizationModule.setGrantedBySaas(true);
        organizationModule.setEnabledByOrganization(true);
        applyDisplayOrder(organizationModule, organizationId, displayOrder);

        return organizationModuleMapper.toDto(organizationModuleRepository.save(organizationModule));
    }

    @Transactional
    public OrganizationModuleDto updateModuleVisibilityForOrganization(
            Integer organizationId,
            String moduleCode,
            Boolean enabledByOrganization
    ) {
        getOrganizationOrThrow(organizationId);
        Module module = moduleService.getModuleByCode(moduleCode);
        OrganizationModule organizationModule = organizationModuleRepository
                .findByOrganizationIdAndModuleIdAndGrantedBySaasTrue(organizationId, module.getId())
                .orElseThrow(() -> new NoSuchElementException("Granted module not found for organization"));

        organizationModule.setEnabledByOrganization(enabledByOrganization);
        return organizationModuleMapper.toDto(organizationModuleRepository.save(organizationModule));
    }

    @Transactional
    public OrganizationModuleDto updateTenantModuleVisibilityForOrganization(
            Integer organizationId,
            String moduleCode,
            Boolean enabledByOrganization
    ) {
        tenantAccessService.assertOrganizationAccessOrThrow(organizationId);
        return updateModuleVisibilityForOrganization(organizationId, moduleCode, enabledByOrganization);
    }

    @Transactional
    public List<OrganizationModuleDto> removeModuleFromOrganization(Integer organizationId, String moduleReference) {
        getOrganizationOrThrow(organizationId);
        Module module = resolveModuleReference(moduleReference);

        OrganizationModule organizationModule = organizationModuleRepository
                .findByOrganizationIdAndModuleIdAndGrantedBySaasTrue(organizationId, module.getId())
                .orElseThrow(() -> new NoSuchElementException("Module is not assigned to this organization"));

        organizationModuleRepository.delete(organizationModule);
        return getAllModulesForOrganization(organizationId);
    }

    private Organization getOrganizationOrThrow(Integer organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));
    }

    private Module resolveModuleReference(String moduleReference) {
        try {
            return moduleService.getModuleById(Long.valueOf(moduleReference));
        } catch (NumberFormatException ignored) {
            return moduleService.getModuleByCode(moduleReference);
        }
    }

    private void applyDisplayOrder(OrganizationModule organizationModule, Integer organizationId, Integer displayOrder) {
        if (displayOrder != null) {
            organizationModule.setDisplayOrder(displayOrder);
            return;
        }

        if (organizationModule.getDisplayOrder() != null) {
            return;
        }

        int nextDisplayOrder = organizationModuleRepository.findByOrganizationIdAndGrantedBySaasTrue(organizationId)
                .stream()
                .map(OrganizationModule::getDisplayOrder)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        organizationModule.setDisplayOrder(nextDisplayOrder);
    }

    private List<OrganizationModuleDto> mapOrganizationModules(List<OrganizationModule> organizationModules) {
        return organizationModules.stream()
                .sorted(
                        Comparator
                                .comparing(
                                        OrganizationModule::getDisplayOrder,
                                        Comparator.nullsLast(Integer::compareTo)
                                )
                                .thenComparing(organizationModule -> organizationModule.getModule().getCode())
                )
                .map(organizationModuleMapper::toDto)
                .toList();
    }
}
