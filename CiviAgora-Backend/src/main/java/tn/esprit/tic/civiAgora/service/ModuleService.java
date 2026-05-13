package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dto.saasDto.SaasModuleCatalogItemDto;
import tn.esprit.tic.civiAgora.dto.saasDto.SaasModuleUpsertRequest;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationModuleRepository organizationModuleRepository;

    public List<Module> getAllModules() {
        return moduleRepository.findAll();
    }

    public Module getModuleByCode(String code) {
        return moduleRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Module not found with code: " + code));
    }

    public Module getModuleById(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Module not found with id: " + id));
    }

    public Module createModule(Module module) {
        if (moduleRepository.findByCode(module.getCode()).isPresent()) {
            throw new RuntimeException("Module code already exists: " + module.getCode());
        }
        return moduleRepository.save(module);
    }

    public List<SaasModuleCatalogItemDto> getSaasCatalog() {
        long totalOrganizations = organizationRepository.count();

        return moduleRepository.findAll().stream()
                .map(module -> {
                    SaasModuleCatalogItemDto dto = new SaasModuleCatalogItemDto();
                    dto.setId(module.getId());
                    dto.setCode(module.getCode());
                    dto.setName(module.getName());
                    dto.setDescription(module.getDescription());
                    dto.setActive(Boolean.TRUE.equals(module.getActive()));
                    dto.setOrganizationsUsing(
                            organizationModuleRepository.countByModuleIdAndGrantedBySaasTrue(module.getId())
                    );
                    dto.setTotalOrganizations(totalOrganizations);
                    return dto;
                })
                .toList();
    }

    public Module createSaasModule(SaasModuleUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Module payload is required");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Module code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Module name is required");
        }

        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (moduleRepository.findByCode(code).isPresent()) {
            throw new IllegalStateException("Module code already exists: " + code);
        }

        Module module = Module.builder()
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription() == null ? null : request.getDescription().trim())
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();

        return moduleRepository.save(module);
    }

    public Module updateSaasModule(Long id, SaasModuleUpsertRequest request) {
        Module module = getModuleById(id);
        if (request == null) {
            throw new IllegalArgumentException("Module payload is required");
        }

        if (request.getCode() != null && !request.getCode().isBlank()) {
            String code = request.getCode().trim().toUpperCase(Locale.ROOT);
            moduleRepository.findByCode(code)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalStateException("Module code already exists: " + code);
                    });
            module.setCode(code);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            module.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            module.setDescription(request.getDescription().trim());
        }
        if (request.getActive() != null) {
            module.setActive(request.getActive());
        }

        return moduleRepository.save(module);
    }
}
