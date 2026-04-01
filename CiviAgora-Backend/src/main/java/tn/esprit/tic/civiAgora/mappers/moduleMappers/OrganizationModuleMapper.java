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
                .grantedBySaas(entity.getGrantedBySaas())
                .enabledByOrganization(entity.getEnabledByOrganization())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}