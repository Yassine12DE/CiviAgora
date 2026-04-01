package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;

    public List<Module> getAllModules() {
        return moduleRepository.findAll();
    }

    public Module getModuleByCode(String code) {
        return moduleRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Module not found with code: " + code));
    }

    public Module createModule(Module module) {
        if (moduleRepository.findByCode(module.getCode()).isPresent()) {
            throw new RuntimeException("Module code already exists: " + module.getCode());
        }
        return moduleRepository.save(module);
    }
}