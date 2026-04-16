package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationStatus;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dao.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrganizationProvisioningService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsService organizationSettingsService;
    private final OrganizationModuleRepository organizationModuleRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public Organization activateRequest(OrganizationRequest request) {
        if (request.getOrganizationCreatedId() != null) {
            return organizationRepository.findById(request.getOrganizationCreatedId())
                    .orElseThrow(() -> new RuntimeException("Created organization is no longer available"));
        }

        String slug = generateUniqueSlug(request.getDesiredSlug());
        if (userRepository.existsByEmail(resolveAdminEmail(request))) {
            throw new IllegalStateException("Admin email already exists");
        }

        Organization organization = new Organization();
        organization.setName(request.getOrganizationName());
        organization.setSlug(slug);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setCreatedAt(LocalDateTime.now());
        organization.setUsersCount(0);
        organization.setProcessesCount(0);
        organization.setEmail(request.getContactEmail());
        organization.setPhone(request.getPhone());
        organization.setAddress(request.getAddress());
        organization.setDescription(request.getDescription());
        organization.setOrganizationLogoUrl(request.getLogoUrl());

        Organization createdOrganization = organizationRepository.save(organization);
        organizationSettingsService.createDefaultSettingsForOrganization(
                createdOrganization,
                request.getRequestedPrimaryColor(),
                request.getRequestedSecondaryColor()
        );
        assignModules(createdOrganization, request.getRequestedModuleCodes());
        userService.createInitialAdminForOrganization(createdOrganization, request);
        request.setOrganizationCreatedId(createdOrganization.getId());

        return createdOrganization;
    }

    public String generateUniqueSlug(String desiredSlug) {
        String base = desiredSlug == null ? "" : desiredSlug.trim().toLowerCase(Locale.ROOT);
        base = base.replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (base.isBlank()) {
            base = "org" + System.currentTimeMillis();
        }

        String slug = base;
        int suffix = 1;

        while (organizationRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + suffix;
            suffix++;
        }

        return slug;
    }

    private void assignModules(Organization organization, List<String> requestedModuleCodes) {
        List<Module> modules = resolveRequestedModules(requestedModuleCodes);
        int displayOrder = 1;

        for (Module module : modules) {
            OrganizationModule organizationModule = OrganizationModule.builder()
                    .organization(organization)
                    .module(module)
                    .grantedBySaas(true)
                    .enabledByOrganization(true)
                    .displayOrder(displayOrder++)
                    .build();
            organizationModuleRepository.save(organizationModule);
        }
    }

    private List<Module> resolveRequestedModules(List<String> requestedModuleCodes) {
        if (requestedModuleCodes == null || requestedModuleCodes.isEmpty()) {
            return moduleRepository.findAll()
                    .stream()
                    .filter(module -> Boolean.TRUE.equals(module.getActive()))
                    .toList();
        }

        return requestedModuleCodes.stream()
                .map(code -> moduleRepository.findByCode(code)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown module code: " + code)))
                .filter(module -> Boolean.TRUE.equals(module.getActive()))
                .toList();
    }

    private String resolveAdminEmail(OrganizationRequest request) {
        return request.getAdminEmail() != null && !request.getAdminEmail().isBlank()
                ? request.getAdminEmail()
                : request.getContactEmail();
    }
}
