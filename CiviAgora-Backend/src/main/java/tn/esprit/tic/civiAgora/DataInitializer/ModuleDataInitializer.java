package tn.esprit.tic.civiAgora.DataInitializer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleScope;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRepository;

@Component
@RequiredArgsConstructor
public class ModuleDataInitializer implements CommandLineRunner {

    private final ModuleRepository moduleRepository;

    @Override
    public void run(String... args) {
        createOrUpdateModule("VOTE", "Voting", "Online votes, polls, eligibility controls, and result publishing.", ModuleScope.BOTH);
        createOrUpdateModule("CONFERENCE", "Concertation", "Public consultation spaces with moderation and discussion threads.", ModuleScope.BOTH);
        createOrUpdateModule("YOUTHSPACE", "Youth Space", "Dedicated civic participation area for youth programs.", ModuleScope.BOTH);
        createOrUpdateModule("EVENTS", "Events", "Event publishing, registrations, attendance, and reminders.", ModuleScope.BOTH);
        createOrUpdateModule("SURVEYS", "Surveys", "Structured questionnaires with exports and branching logic.", ModuleScope.BOTH);
        createOrUpdateModule("COMPLAINTS", "Complaints", "Issue reporting, routing, and public service tracking.", ModuleScope.BOTH);
        createOrUpdateModule("NEWS", "News", "Official announcements and tenant news feeds.", ModuleScope.BOTH);
        createOrUpdateModule("ANALYTICS", "Analytics", "Participation analytics, exports, and executive dashboards.", ModuleScope.BACK_OFFICE);
    }

    private void createOrUpdateModule(String code, String name, String description, ModuleScope scope) {
        moduleRepository.findByCode(code).ifPresentOrElse(existing -> {
            ModuleScope existingScope = ModuleScope.resolveOrDefault(existing.getScope());
            if (existingScope != scope) {
                existing.setScope(scope);
                moduleRepository.save(existing);
            }
        }, () -> moduleRepository.save(
                Module.builder()
                        .code(code)
                        .name(name)
                        .description(description)
                        .scope(scope)
                        .active(true)
                        .build()
        ));
    }
}
