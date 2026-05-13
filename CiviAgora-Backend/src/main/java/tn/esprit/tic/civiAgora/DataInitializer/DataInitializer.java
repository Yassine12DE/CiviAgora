package tn.esprit.tic.civiAgora.DataInitializer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.ModuleRequest;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationSettings;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleRequestStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.PaymentStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.QuoteStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRequestRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRequestRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationSettingsRepository;
import tn.esprit.tic.civiAgora.dao.repository.UserRepository;
import tn.esprit.tic.civiAgora.service.SaasPlatformSettingsService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "civox.seed.demo-data", havingValue = "true")
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;
    private final SaasPlatformSettingsService saasPlatformSettingsService;

    @Bean
    CommandLineRunner seedDatabase(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            OrganizationSettingsRepository organizationSettingsRepository,
            ModuleRepository moduleRepository,
            OrganizationModuleRepository organizationModuleRepository,
            ModuleRequestRepository moduleRequestRepository,
            OrganizationRequestRepository organizationRequestRepository
    ) {
        return args -> {
            Map<String, Organization> organizations = seedOrganizations(organizationRepository);
            seedOrganizationSettings(organizationSettingsRepository, organizations);
            seedUsers(userRepository, organizations);
            seedOrganizationModules(moduleRepository, organizationModuleRepository, organizations);
            seedModuleRequests(moduleRepository, moduleRequestRepository, organizations);
            seedOrganizationRequests(organizationRequestRepository, organizations);
            saasPlatformSettingsService.seedDefaultsIfMissing();

            System.out.println("Seed completed: organizations, users, settings, module access, requests, and SaaS settings are ready.");
        };
    }

    private Map<String, Organization> seedOrganizations(OrganizationRepository organizationRepository) {
        List<OrganizationSeed> seeds = List.of(
                new OrganizationSeed("city-montreal", "City of Montreal", OrganizationStatus.ACTIVE, "admin@montreal.ca", "+1 514 555 0184", "275 Notre-Dame St E, Montreal, QC", "Large municipal tenant running citizen participation workflows.", 510, 128),
                new OrganizationSeed("town-oakville", "Town of Oakville", OrganizationStatus.ACTIVE, "admin@oakville.ca", "+1 905 555 0192", "1225 Trafalgar Rd, Oakville, ON", "Mid-size civic engagement tenant focused on polls and events.", 162, 51),
                new OrganizationSeed("district-xy", "Regional District XY", OrganizationStatus.ACTIVE, "contact@districtxy.ca", "+1 604 555 0144", "480 Civic Centre Rd, Victoria, BC", "Regional tenant coordinating youth programs and consultations.", 214, 63),
                new OrganizationSeed("municipality-laval", "Municipality of Laval", OrganizationStatus.ACTIVE, "platform@laval.ca", "+1 450 555 0110", "1 Place du Souvenir, Laval, QC", "Enterprise tenant operating feedback and service reporting.", 358, 99),
                new OrganizationSeed("city-burlington", "City of Burlington", OrganizationStatus.INACTIVE, "digital@burlington.ca", "+1 289 555 0107", "426 Brant St, Burlington, ON", "Starter tenant currently inactive pending billing follow-up.", 72, 18),
                new OrganizationSeed("town-markham", "Town of Markham", OrganizationStatus.PENDING, "civic@markham.ca", "+1 905 555 0138", "101 Town Centre Blvd, Markham, ON", "Tenant onboarding in progress for expanded consultations.", 0, 0)
        );

        Map<String, Organization> map = new LinkedHashMap<>();
        for (int i = 0; i < seeds.size(); i++) {
            OrganizationSeed seed = seeds.get(i);
            Organization organization = organizationRepository.findBySlug(seed.slug()).orElseGet(Organization::new);

            organization.setSlug(seed.slug());
            organization.setName(seed.name());
            organization.setStatus(seed.status());
            organization.setEmail(seed.email());
            organization.setPhone(seed.phone());
            organization.setAddress(seed.address());
            organization.setDescription(seed.description());
            organization.setProcessesCount(seed.processesCount());
            organization.setUsersCount(seed.usersCount());
            if (organization.getCreatedAt() == null) {
                organization.setCreatedAt(LocalDateTime.now().minusDays(210L - i * 20L));
            }

            Organization saved = organizationRepository.save(organization);
            map.put(saved.getSlug(), saved);
        }

        return map;
    }

    private void seedOrganizationSettings(
            OrganizationSettingsRepository repository,
            Map<String, Organization> organizations
    ) {
        Map<String, String[]> colors = Map.of(
                "city-montreal", new String[]{"#0066CC", "#FF6B35"},
                "town-oakville", new String[]{"#0F766E", "#F59E0B"},
                "district-xy", new String[]{"#2563EB", "#FB7185"},
                "municipality-laval", new String[]{"#7C3AED", "#FF8F66"},
                "city-burlington", new String[]{"#475569", "#F97316"},
                "town-markham", new String[]{"#7B2CBF", "#FF6B35"}
        );

        for (Organization organization : organizations.values()) {
            OrganizationSettings settings = repository.findByOrganizationId(organization.getId())
                    .orElseGet(() -> OrganizationSettings.builder().organization(organization).build());

            String[] palette = colors.getOrDefault(organization.getSlug(), new String[]{"#7B2CBF", "#FF6B35"});
            settings.setLogoUrl("https://dummyimage.com/200x200/ffffff/000000&text=" + organization.getName().replace(" ", "+"));
            settings.setPrimaryColor(palette[0]);
            settings.setSecondaryColor(palette[1]);
            settings.setHomeTitle(organization.getName() + " Civic Platform");
            settings.setWelcomeText("Welcome to " + organization.getName() + ". Participate in local decisions and follow initiatives.");
            settings.setBannerImageUrl("https://dummyimage.com/1400x280/" + palette[0].replace("#", "") + "/ffffff&text=" + organization.getSlug());
            settings.setFooterText(organization.getName() + " digital participation space");

            repository.save(settings);
        }
    }

    private void seedUsers(UserRepository userRepository, Map<String, Organization> organizations) {
        upsertUser(userRepository, "admin@civox.io", "Platform", "Admin", Role.SUPER_ADMIN, null, "+1 555 000 1000");

        upsertUser(userRepository, "sophie.dubois@montreal.ca", "Sophie", "Dubois", Role.ADMIN, organizations.get("city-montreal"), "+1 514 555 1010");
        upsertUser(userRepository, "jean.martin@montreal.ca", "Jean", "Martin", Role.MANAGER, organizations.get("city-montreal"), "+1 514 555 1011");
        upsertUser(userRepository, "camille.roy@montreal.ca", "Camille", "Roy", Role.CITIZEN, organizations.get("city-montreal"), "+1 514 555 1012");

        upsertUser(userRepository, "james.wilson@oakville.ca", "James", "Wilson", Role.ADMIN, organizations.get("town-oakville"), "+1 905 555 1020");
        upsertUser(userRepository, "nina.clark@oakville.ca", "Nina", "Clark", Role.MODERATOR, organizations.get("town-oakville"), "+1 905 555 1021");

        upsertUser(userRepository, "maria.chen@districtxy.ca", "Maria", "Chen", Role.ADMIN, organizations.get("district-xy"), "+1 604 555 1030");
        upsertUser(userRepository, "lucas.arnold@districtxy.ca", "Lucas", "Arnold", Role.MANAGER, organizations.get("district-xy"), "+1 604 555 1031");

        upsertUser(userRepository, "pierre.tremblay@laval.ca", "Pierre", "Tremblay", Role.ADMIN, organizations.get("municipality-laval"), "+1 450 555 1040");
        upsertUser(userRepository, "lea.bernard@laval.ca", "Lea", "Bernard", Role.MODERATOR, organizations.get("municipality-laval"), "+1 450 555 1041");

        upsertUser(userRepository, "robert.anderson@burlington.ca", "Robert", "Anderson", Role.ADMIN, organizations.get("city-burlington"), "+1 289 555 1050");
        upsertUser(userRepository, "amelia.grant@markham.ca", "Amelia", "Grant", Role.ADMIN, organizations.get("town-markham"), "+1 905 555 1060");

        organizations.values().forEach(organization -> {
            int usersCount = userRepository.countByOrganizationId(organization.getId());
            organization.setUsersCount(usersCount);
        });
    }

    private void upsertUser(
            UserRepository userRepository,
            String email,
            String firstName,
            String lastName,
            Role role,
            Organization organization,
            String phone
    ) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);

        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setPhone(phone);
        user.setBirthDate("1990-01-01");
        user.setOrganization(organization);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode("Password123!"));
        }
        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }
        if (user.getArchived() == null) {
            user.setArchived(false);
        }

        userRepository.save(user);
    }

    private void seedOrganizationModules(
            ModuleRepository moduleRepository,
            OrganizationModuleRepository organizationModuleRepository,
            Map<String, Organization> organizations
    ) {
        Map<String, List<String>> modulesByOrg = Map.of(
                "city-montreal", List.of("VOTE", "CONFERENCE", "EVENTS", "SURVEYS", "ANALYTICS"),
                "town-oakville", List.of("VOTE", "EVENTS", "SURVEYS"),
                "district-xy", List.of("CONFERENCE", "YOUTHSPACE", "EVENTS"),
                "municipality-laval", List.of("VOTE", "CONFERENCE", "SURVEYS", "COMPLAINTS", "NEWS"),
                "city-burlington", List.of("EVENTS", "NEWS"),
                "town-markham", List.of("VOTE", "SURVEYS", "YOUTHSPACE")
        );

        for (Map.Entry<String, List<String>> entry : modulesByOrg.entrySet()) {
            Organization organization = organizations.get(entry.getKey());
            if (organization == null) continue;

            int order = 1;
            for (String moduleCode : entry.getValue()) {
                Module module = moduleRepository.findByCode(moduleCode)
                        .orElseThrow(() -> new RuntimeException("Missing module seed: " + moduleCode));

                OrganizationModule organizationModule = organizationModuleRepository
                        .findByOrganizationIdAndModuleId(organization.getId(), module.getId())
                        .orElseGet(() -> OrganizationModule.builder()
                                .organization(organization)
                                .module(module)
                                .grantedBySaas(true)
                                .enabledByOrganization(true)
                                .build());

                organizationModule.setDisplayOrder(order++);
                organizationModule.setGrantedBySaas(true);
                organizationModule.setEnabledByOrganization(true);
                organizationModuleRepository.save(organizationModule);
            }
        }
    }

    private void seedModuleRequests(
            ModuleRepository moduleRepository,
            ModuleRequestRepository moduleRequestRepository,
            Map<String, Organization> organizations
    ) {
        List<ModuleRequestSeed> seeds = List.of(
                new ModuleRequestSeed("city-montreal", "YOUTHSPACE", ModuleRequestStatus.PENDING, "Expanding youth engagement programs.", 2, null),
                new ModuleRequestSeed("town-oakville", "ANALYTICS", ModuleRequestStatus.PENDING, "Need deeper council participation reporting.", 3, null),
                new ModuleRequestSeed("municipality-laval", "ANALYTICS", ModuleRequestStatus.APPROVED, "Executive reporting and trends.", 8, 6),
                new ModuleRequestSeed("district-xy", "VOTE", ModuleRequestStatus.REJECTED, "Online polls for budget consultation.", 10, 9)
        );

        for (ModuleRequestSeed seed : seeds) {
            Organization organization = organizations.get(seed.organizationSlug());
            if (organization == null) continue;

            Module module = moduleRepository.findByCode(seed.moduleCode())
                    .orElseThrow(() -> new RuntimeException("Missing module seed: " + seed.moduleCode()));

            if (moduleRequestRepository.existsByOrganizationIdAndModuleCodeAndStatus(
                    organization.getId(),
                    module.getCode(),
                    seed.status()
            )) {
                continue;
            }

            ModuleRequest request = ModuleRequest.builder()
                    .organization(organization)
                    .module(module)
                    .status(seed.status())
                    .comment(seed.comment())
                    .requestDate(LocalDateTime.now().minusDays(seed.requestedDaysAgo()))
                    .reviewedDate(seed.reviewedDaysAgo() == null ? null : LocalDateTime.now().minusDays(seed.reviewedDaysAgo()))
                    .build();

            moduleRequestRepository.save(request);
        }
    }

    private void seedOrganizationRequests(
            OrganizationRequestRepository repository,
            Map<String, Organization> organizations
    ) {
        upsertOrganizationRequest(repository, OrganizationRequestSeed.pending(
                "rc-west",
                "Regional Council West",
                "Amelia Grant",
                "amelia.grant@rc-west.ca",
                "admin@rc-west.ca",
                List.of("VOTE", "EVENTS", "SURVEYS"),
                150,
                "+1 403 555 0165",
                "22 Regional Way, Calgary, AB",
                "Regional government office preparing a citizen participation launch.",
                6
        ));

        upsertOrganizationRequest(repository, OrganizationRequestSeed.quoteSent(
                "town-aurora",
                "Town of Aurora",
                "Nadia Patel",
                "tech@aurora.ca",
                "nadia.patel@aurora.ca",
                List.of("CONFERENCE", "YOUTHSPACE", "NEWS"),
                200,
                "+1 905 555 0103",
                "100 John West Way, Aurora, ON",
                "Town team requesting consultation and youth engagement modules.",
                12,
                new BigDecimal("4200.00")
        ));

        upsertOrganizationRequest(repository, OrganizationRequestSeed.awaitingPayment(
                "city-stratford",
                "City of Stratford",
                "Evan Brooks",
                "civic@stratford.ca",
                "evan.brooks@stratford.ca",
                List.of("VOTE", "CONFERENCE", "SURVEYS", "EVENTS"),
                300,
                "+1 519 555 0175",
                "1 Wellington St, Stratford, ON",
                "City office ready to activate voting and consultation modules.",
                18,
                new BigDecimal("5700.00")
        ));

        Organization activatedOrganization = organizations.get("town-markham");
        upsertOrganizationRequest(repository, OrganizationRequestSeed.activated(
                "district-board-east",
                "District Board East",
                "Lina Torres",
                "contact@dbe.org",
                "lina.torres@dbe.org",
                List.of("YOUTHSPACE", "EVENTS"),
                100,
                "+1 416 555 0188",
                "88 Education Ave, Toronto, ON",
                "Educational institution running events and youth consultations.",
                24,
                new BigDecimal("2750.00"),
                activatedOrganization != null ? activatedOrganization.getId() : null
        ));
    }

    private void upsertOrganizationRequest(OrganizationRequestRepository repository, OrganizationRequestSeed seed) {
        OrganizationRequest request = repository.findByDesiredSlug(seed.desiredSlug()).orElseGet(OrganizationRequest::new);

        request.setOrganizationName(seed.organizationName());
        request.setDesiredSlug(seed.desiredSlug());
        request.setContactPersonName(seed.contactPersonName());
        request.setContactEmail(seed.contactEmail());
        request.setPhone(seed.phone());
        request.setAddress(seed.address());
        request.setDescription(seed.description());
        request.setAdminFirstName(seed.contactPersonName().split(" ")[0]);
        request.setAdminLastName(seed.contactPersonName().contains(" ") ? seed.contactPersonName().substring(seed.contactPersonName().indexOf(' ') + 1) : "Admin");
        request.setAdminEmail(seed.adminEmail());
        if (request.getAdminPasswordHash() == null || request.getAdminPasswordHash().isBlank()) {
            request.setAdminPasswordHash(passwordEncoder.encode("TempPass@123"));
        }
        request.setExpectedNumberOfUsers(seed.expectedUsers());
        request.setRequestedModuleCodes(seed.moduleCodes());
        request.setRequestStatus(seed.requestStatus());
        request.setQuoteStatus(seed.quoteStatus());
        request.setPaymentStatus(seed.paymentStatus());
        request.setQuoteBaseFee(seed.quoteBaseFee());
        request.setQuoteUserFee(seed.quoteUserFee());
        request.setQuoteModuleFee(seed.quoteModuleFee());
        request.setQuoteSetupFee(seed.quoteSetupFee());
        request.setQuoteTotal(seed.quoteTotal());
        request.setQuoteAssumptions(seed.quoteAssumptions());
        request.setCreatedAt(LocalDateTime.now().minusDays(seed.createdDaysAgo()));
        request.setUpdatedAt(LocalDateTime.now().minusDays(Math.max(1, seed.createdDaysAgo() - 1)));
        request.setQuoteSentAt(seed.quoteSentAt());
        request.setApprovedAt(seed.approvedAt());
        request.setPaidAt(seed.paidAt());
        request.setActivatedAt(seed.activatedAt());
        request.setDeclinedAt(seed.declinedAt());
        request.setOrganizationCreatedId(seed.organizationCreatedId());

        repository.save(request);
    }

    private record OrganizationSeed(
            String slug,
            String name,
            OrganizationStatus status,
            String email,
            String phone,
            String address,
            String description,
            int usersCount,
            int processesCount
    ) {
    }

    private record ModuleRequestSeed(
            String organizationSlug,
            String moduleCode,
            ModuleRequestStatus status,
            String comment,
            int requestedDaysAgo,
            Integer reviewedDaysAgo
    ) {
    }

    private record OrganizationRequestSeed(
            String desiredSlug,
            String organizationName,
            String contactPersonName,
            String contactEmail,
            String adminEmail,
            List<String> moduleCodes,
            int expectedUsers,
            String phone,
            String address,
            String description,
            int createdDaysAgo,
            OrganizationRequestStatus requestStatus,
            QuoteStatus quoteStatus,
            PaymentStatus paymentStatus,
            BigDecimal quoteBaseFee,
            BigDecimal quoteUserFee,
            BigDecimal quoteModuleFee,
            BigDecimal quoteSetupFee,
            BigDecimal quoteTotal,
            String quoteAssumptions,
            LocalDateTime quoteSentAt,
            LocalDateTime approvedAt,
            LocalDateTime paidAt,
            LocalDateTime activatedAt,
            LocalDateTime declinedAt,
            Integer organizationCreatedId
    ) {
        static OrganizationRequestSeed pending(
                String desiredSlug,
                String organizationName,
                String contactPersonName,
                String contactEmail,
                String adminEmail,
                List<String> moduleCodes,
                int expectedUsers,
                String phone,
                String address,
                String description,
                int createdDaysAgo
        ) {
            return new OrganizationRequestSeed(
                    desiredSlug,
                    organizationName,
                    contactPersonName,
                    contactEmail,
                    adminEmail,
                    moduleCodes,
                    expectedUsers,
                    phone,
                    address,
                    description,
                    createdDaysAgo,
                    OrganizationRequestStatus.PENDING,
                    QuoteStatus.NOT_CREATED,
                    PaymentStatus.NOT_STARTED,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        static OrganizationRequestSeed quoteSent(
                String desiredSlug,
                String organizationName,
                String contactPersonName,
                String contactEmail,
                String adminEmail,
                List<String> moduleCodes,
                int expectedUsers,
                String phone,
                String address,
                String description,
                int createdDaysAgo,
                BigDecimal quoteTotal
        ) {
            LocalDateTime quoteDate = LocalDateTime.now().minusDays(createdDaysAgo - 1L);
            return quoted(
                    desiredSlug,
                    organizationName,
                    contactPersonName,
                    contactEmail,
                    adminEmail,
                    moduleCodes,
                    expectedUsers,
                    phone,
                    address,
                    description,
                    createdDaysAgo,
                    OrganizationRequestStatus.QUOTE_SENT,
                    QuoteStatus.SENT,
                    PaymentStatus.NOT_STARTED,
                    quoteDate,
                    quoteTotal,
                    null,
                    null,
                    null,
                    null
            );
        }

        static OrganizationRequestSeed awaitingPayment(
                String desiredSlug,
                String organizationName,
                String contactPersonName,
                String contactEmail,
                String adminEmail,
                List<String> moduleCodes,
                int expectedUsers,
                String phone,
                String address,
                String description,
                int createdDaysAgo,
                BigDecimal quoteTotal
        ) {
            LocalDateTime quoteDate = LocalDateTime.now().minusDays(createdDaysAgo - 1L);
            LocalDateTime approvedDate = LocalDateTime.now().minusDays(createdDaysAgo - 1L);
            return quoted(
                    desiredSlug,
                    organizationName,
                    contactPersonName,
                    contactEmail,
                    adminEmail,
                    moduleCodes,
                    expectedUsers,
                    phone,
                    address,
                    description,
                    createdDaysAgo,
                    OrganizationRequestStatus.AWAITING_PAYMENT,
                    QuoteStatus.ACCEPTED,
                    PaymentStatus.AWAITING_PAYMENT,
                    quoteDate,
                    quoteTotal,
                    approvedDate,
                    null,
                    null,
                    null
            );
        }

        static OrganizationRequestSeed activated(
                String desiredSlug,
                String organizationName,
                String contactPersonName,
                String contactEmail,
                String adminEmail,
                List<String> moduleCodes,
                int expectedUsers,
                String phone,
                String address,
                String description,
                int createdDaysAgo,
                BigDecimal quoteTotal,
                Integer organizationCreatedId
        ) {
            LocalDateTime quoteDate = LocalDateTime.now().minusDays(createdDaysAgo - 2L);
            LocalDateTime approvedDate = LocalDateTime.now().minusDays(createdDaysAgo - 2L);
            LocalDateTime paidDate = LocalDateTime.now().minusDays(createdDaysAgo - 1L);
            LocalDateTime activatedDate = LocalDateTime.now().minusDays(createdDaysAgo - 1L);
            return quoted(
                    desiredSlug,
                    organizationName,
                    contactPersonName,
                    contactEmail,
                    adminEmail,
                    moduleCodes,
                    expectedUsers,
                    phone,
                    address,
                    description,
                    createdDaysAgo,
                    OrganizationRequestStatus.APPROVED,
                    QuoteStatus.ACCEPTED,
                    PaymentStatus.PAID,
                    quoteDate,
                    quoteTotal,
                    approvedDate,
                    paidDate,
                    activatedDate,
                    organizationCreatedId
            );
        }

        private static OrganizationRequestSeed quoted(
                String desiredSlug,
                String organizationName,
                String contactPersonName,
                String contactEmail,
                String adminEmail,
                List<String> moduleCodes,
                int expectedUsers,
                String phone,
                String address,
                String description,
                int createdDaysAgo,
                OrganizationRequestStatus requestStatus,
                QuoteStatus quoteStatus,
                PaymentStatus paymentStatus,
                LocalDateTime quoteSentAt,
                BigDecimal quoteTotal,
                LocalDateTime approvedAt,
                LocalDateTime paidAt,
                LocalDateTime activatedAt,
                Integer organizationCreatedId
        ) {
            BigDecimal base = quoteTotal.multiply(new BigDecimal("0.35"));
            BigDecimal users = quoteTotal.multiply(new BigDecimal("0.25"));
            BigDecimal modules = quoteTotal.multiply(new BigDecimal("0.25"));
            BigDecimal setup = quoteTotal.subtract(base).subtract(users).subtract(modules);

            return new OrganizationRequestSeed(
                    desiredSlug,
                    organizationName,
                    contactPersonName,
                    contactEmail,
                    adminEmail,
                    moduleCodes,
                    expectedUsers,
                    phone,
                    address,
                    description,
                    createdDaysAgo,
                    requestStatus,
                    quoteStatus,
                    paymentStatus,
                    base,
                    users,
                    modules,
                    setup,
                    quoteTotal,
                    "Annual estimate with onboarding and requested modules.",
                    quoteSentAt,
                    approvedAt,
                    paidAt,
                    activatedAt,
                    null,
                    organizationCreatedId
            );
        }
    }
}
