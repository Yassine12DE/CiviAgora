package tn.esprit.tic.civiAgora.mappers.moduleMappers;

import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;

@Component
public class OrganizationModuleMapper {

    public OrganizationModuleDto toDto(OrganizationModule entity) {
        if (entity == null) return null;

        return OrganizationModuleDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization().getId())
                .moduleId(entity.getModule().getId())
                .moduleCode(entity.getModule().getCode())
                .moduleName(entity.getModule().getName())
                .moduleDescription(entity.getModule().getDescription())
                .moduleScope(entity.getModule().getScope() == null ? null : entity.getModule().getScope().name())
                .billingType(entity.getModule().getBillingType() == null ? null : entity.getModule().getBillingType().name())
                .grantedBySaas(entity.getGrantedBySaas())
                .enabledByOrganization(entity.getEnabledByOrganization())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}
