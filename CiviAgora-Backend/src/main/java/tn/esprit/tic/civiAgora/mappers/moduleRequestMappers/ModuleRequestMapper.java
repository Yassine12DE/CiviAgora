package tn.esprit.tic.civiAgora.mappers.moduleRequestMappers;

import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.ModuleRequest;
import tn.esprit.tic.civiAgora.dto.moduleRequestDto.ModuleRequestDto;

@Component
public class ModuleRequestMapper {

    public ModuleRequestDto toDto(ModuleRequest entity) {
        if (entity == null) return null;

        return ModuleRequestDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization().getId())
                .organizationName(entity.getOrganization().getName())
                .moduleId(entity.getModule().getId())
                .moduleCode(entity.getModule().getCode())
                .moduleName(entity.getModule().getName())
                .status(entity.getStatus().name())
                .requestDate(entity.getRequestDate())
                .reviewedDate(entity.getReviewedDate())
                .comment(entity.getComment())
                .build();
    }
}