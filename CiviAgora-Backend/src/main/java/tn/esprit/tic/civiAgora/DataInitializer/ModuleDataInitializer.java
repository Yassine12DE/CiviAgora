package tn.esprit.tic.civiAgora.DataInitializer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleBillingType;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleScope;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRepository;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ModuleDataInitializer implements CommandLineRunner {

    private final ModuleRepository moduleRepository;

    @Override
    public void run(String... args) {
        createOrUpdateModule("VOTE", "Voting", "Online votes, polls, eligibility controls, and result publishing.", ModuleScope.BOTH, ModuleBillingType.ONE_TIME, new BigDecimal("350.00"), new BigDecimal("45.00"), new BigDecimal("420.00"));
        createOrUpdateModule("CONFERENCE", "Concertation", "Public consultation spaces with moderation and discussion threads.", ModuleScope.BOTH, ModuleBillingType.ONE_TIME, new BigDecimal("420.00"), new BigDecimal("55.00"), new BigDecimal("520.00"));
        createOrUpdateModule("YOUTHSPACE", "Youth Space", "Dedicated civic participation area for youth programs.", ModuleScope.BOTH, ModuleBillingType.ONE_TIME, new BigDecimal("300.00"), new BigDecimal("39.00"), new BigDecimal("390.00"));
        createOrUpdateModule("EVENTS", "Events", "Event publishing, registrations, attendance, and reminders.", ModuleScope.BOTH, ModuleBillingType.ONE_TIME, new BigDecimal("280.00"), new BigDecimal("35.00"), new BigDecimal("360.00"));
        createOrUpdateModule("SURVEYS", "Surveys", "Structured questionnaires with exports and branching logic.", ModuleScope.BOTH, ModuleBillingType.ONE_TIME, new BigDecimal("260.00"), new BigDecimal("32.00"), new BigDecimal("330.00"));
        createOrUpdateModule("COMPLAINTS", "Complaints", "Issue reporting, routing, and public service tracking.", ModuleScope.BOTH, ModuleBillingType.ONE_TIME, new BigDecimal("390.00"), new BigDecimal("50.00"), new BigDecimal("470.00"));
        createOrUpdateModule("NEWS", "News", "Official announcements and tenant news feeds.", ModuleScope.BOTH, ModuleBillingType.ONE_TIME, new BigDecimal("240.00"), new BigDecimal("30.00"), new BigDecimal("300.00"));
        createOrUpdateModule("ANALYTICS", "Analytics", "Participation analytics, exports, and executive dashboards.", ModuleScope.BACK_OFFICE, ModuleBillingType.ONE_TIME, new BigDecimal("500.00"), new BigDecimal("65.00"), new BigDecimal("600.00"));
    }

    private void createOrUpdateModule(
            String code,
            String name,
            String description,
            ModuleScope scope,
            ModuleBillingType billingType,
            BigDecimal oneTimePrice,
            BigDecimal monthlyPrice,
            BigDecimal yearlyPrice
    ) {
        moduleRepository.findByCode(code).ifPresentOrElse(existing -> {
            ModuleScope existingScope = ModuleScope.resolveOrDefault(existing.getScope());
            if (existingScope != scope) {
                existing.setScope(scope);
            }
            existing.setBillingType(billingType);
            existing.setOneTimePrice(oneTimePrice);
            existing.setMonthlyPrice(monthlyPrice);
            existing.setYearlyPrice(yearlyPrice);
            existing.setDescription(description);
            existing.setName(name);
            moduleRepository.save(existing);
        }, () -> moduleRepository.save(
                Module.builder()
                        .code(code)
                        .name(name)
                        .description(description)
                        .scope(scope)
                        .billingType(billingType)
                        .oneTimePrice(oneTimePrice)
                        .monthlyPrice(monthlyPrice)
                        .yearlyPrice(yearlyPrice)
                        .active(true)
                        .build()
        ));
    }
}
