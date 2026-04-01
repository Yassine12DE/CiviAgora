package tn.esprit.tic.civiAgora.mappers.moduleMappers;

import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dto.moduleDto.ModuleDto;

@Component
public class ModuleMapper {

    public ModuleDto toDto(Module module) {
        if (module == null) return null;

        return ModuleDto.builder()
                .id(module.getId())
                .code(module.getCode())
                .name(module.getName())
                .description(module.getDescription())
                .active(module.getActive())
                .build();
    }
}