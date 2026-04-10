package tn.esprit.tic.civiAgora.DataInitializer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationSettings;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationSettingsRepository;
import tn.esprit.tic.civiAgora.dao.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedDatabase(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            OrganizationSettingsRepository organizationSettingsRepository
    ) {
        return args -> {

            List<Organization> organizations = new ArrayList<>();

            /* ===================== 10 ORGANIZATIONS ===================== */

            for (int i = 1; i <= 10; i++) {

                final int index = i;
                String slug = "org-" + index;

                Organization org = organizationRepository.findBySlug(slug).orElse(null);

                if (org == null) {
                    org = new Organization();
                    org.setName("Organization " + index);
                    org.setSlug(slug);
                    org.setStatus(
                            index % 3 == 0 ? OrganizationStatus.PENDING :
                                    index % 2 == 0 ? OrganizationStatus.INACTIVE :
                                            OrganizationStatus.ACTIVE
                    );
                    org.setCreatedAt(LocalDateTime.now().minusDays(index * 5L));
                    org.setEmail("contact@org" + index + ".com");
                    org.setPhone("+2167000000" + index);
                    org.setAddress("City " + index + ", Tunisia");
                    org.setDescription("Test organization number " + index);

                    org = organizationRepository.save(org);
                }

                organizations.add(org);
            }

            /* ===================== ORGANIZATION SETTINGS ===================== */

            String[][] colors = {
                    {"#1E3A8A", "#60A5FA"},
                    {"#0F766E", "#2DD4BF"},
                    {"#7C3AED", "#A78BFA"},
                    {"#B45309", "#F59E0B"},
                    {"#BE123C", "#FB7185"},
                    {"#166534", "#4ADE80"},
                    {"#1D4ED8", "#93C5FD"},
                    {"#6D28D9", "#C4B5FD"},
                    {"#0F172A", "#64748B"},
                    {"#9A3412", "#FDBA74"}
            };

            for (int i = 0; i < organizations.size(); i++) {
                Organization org = organizations.get(i);

                boolean settingsExist = organizationSettingsRepository
                        .findByOrganizationId(org.getId())
                        .isPresent();

                if (settingsExist) {
                    continue;
                }

                OrganizationSettings settings = OrganizationSettings.builder()
                        .organization(org)
                        .logoUrl("https://dummyimage.com/200x200/ffffff/000000&text=ORG+" + (i + 1))
                        .primaryColor(colors[i][0])
                        .secondaryColor(colors[i][1])
                        .homeTitle("Welcome to " + org.getName())
                        .welcomeText("This is the official workspace of " + org.getName() + ". Access your modules, updates, and organization services here.")
                        .bannerImageUrl("https://dummyimage.com/1200x300/" +
                                colors[i][0].replace("#", "") + "/ffffff&text=" + org.getSlug())
                        .footerText("© 2026 " + org.getName() + " - Powered by Civox")
                        .build();

                organizationSettingsRepository.save(settings);
            }

            /* ===================== 20 USERS ===================== */

            Role[] roles = {
                    Role.SUPER_ADMIN,
                    Role.ADMIN,
                    Role.MANAGER,
                    Role.MODERATOR,
                    Role.CITIZEN,
                    Role.CITIZEN,
                    Role.CITIZEN,
                    Role.OBSERVER,
                    Role.ADMIN,
                    Role.MANAGER
            };

            for (int i = 1; i <= 20; i++) {

                final int index = i;
                String email = "user" + index + "@civiagora.tn";

                if (userRepository.existsByEmail(email)) {
                    continue;
                }

                User user = User.builder()
                        .firstName("User" + index)
                        .lastName("Test")
                        .email(email)
                        .password(passwordEncoder.encode("Password123"))
                        .role(roles[(index - 1) % roles.length])
                        .organization(organizations.get((index - 1) % organizations.size()))
                        .phone("9000001" + index)
                        .birthDate("199" + (index % 10) + "-0" + ((index % 9) + 1) + "-15")
                        .build();

                userRepository.save(user);
            }

            System.out.println("✅ 10 organizations + organization settings + 20 users safely added");
        };
    }
}