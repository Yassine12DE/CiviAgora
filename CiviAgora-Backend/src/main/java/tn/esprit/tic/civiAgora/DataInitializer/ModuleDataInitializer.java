package tn.esprit.tic.civiAgora.DataInitializer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRepository;

@Component
@RequiredArgsConstructor
public class ModuleDataInitializer implements CommandLineRunner {

    private final ModuleRepository moduleRepository;

    @Override
    public void run(String... args) {
        createModuleIfNotExists("VOTE", "Vote", "Create polls and let organization users vote.");
        createModuleIfNotExists("CONFERENCE", "Conference", "Create concertations and manage participation.");
        createModuleIfNotExists("YOUTHSPACE", "YouthSpace", "Publish news for youth users.");
    }

    private void createModuleIfNotExists(String code, String name, String description) {
        moduleRepository.findByCode(code).orElseGet(() ->
                moduleRepository.save(
                        Module.builder()
                                .code(code)
                                .name(name)
                                .description(description)
                                .active(true)
                                .build()
                )
        );
    }
}