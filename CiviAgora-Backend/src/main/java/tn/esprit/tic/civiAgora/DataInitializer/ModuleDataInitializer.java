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
        createModuleIfNotExists("VOTE", "Voting", "Online votes, polls, eligibility controls, and result publishing.");
        createModuleIfNotExists("CONFERENCE", "Concertation", "Public consultation spaces with moderation and discussion threads.");
        createModuleIfNotExists("YOUTHSPACE", "Youth Space", "Dedicated civic participation area for youth programs.");
        createModuleIfNotExists("EVENTS", "Events", "Event publishing, registrations, attendance, and reminders.");
        createModuleIfNotExists("SURVEYS", "Surveys", "Structured questionnaires with exports and branching logic.");
        createModuleIfNotExists("COMPLAINTS", "Complaints", "Issue reporting, routing, and public service tracking.");
        createModuleIfNotExists("NEWS", "News", "Official announcements and tenant news feeds.");
        createModuleIfNotExists("ANALYTICS", "Analytics", "Participation analytics, exports, and executive dashboards.");
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
