package tn.esprit.tic.civiAgora.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationStatus;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dao.repository.UserRepository;
import tn.esprit.tic.civiAgora.dto.organizationDto.OrganizationDto;
import tn.esprit.tic.civiAgora.mappers.organizationMappers.OrganizationMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganizationService {
    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private OrganizationSettingsService organizationSettingsService ;


    public List<OrganizationDto> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(org -> {
                    int usersCount = (int) userRepository.countUsersByOrganization(org.getId());
                    return organizationMapper.toOrganizationDto(org, usersCount);
                })
                .toList();
    }

    // Get organization by ID
    public Organization getOrganizationById(Integer id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with ID: " + id));
    }

    // Create a new organization
    public Organization createOrganization(Organization organization) {
        organization.setCreatedAt(LocalDateTime.now());
        organizationSettingsService.createDefaultSettingsForOrganization(organization);

        return organizationRepository.save(organization);
    }

    // Update an existing organization
    public Organization updateOrganization(Integer id, Organization updatedOrg) {
        Organization organization = getOrganizationById(id);

        organization.setName(updatedOrg.getName());
        organization.setSlug(updatedOrg.getSlug());
        organization.setStatus(updatedOrg.getStatus());
        organization.setEmail(updatedOrg.getEmail());
        organization.setPhone(updatedOrg.getPhone());
        organization.setAddress(updatedOrg.getAddress());
        organization.setDescription(updatedOrg.getDescription());

        return organizationRepository.save(organization);
    }

    // Delete organization by ID
    public void deleteOrganization(Integer id) {
        Organization org = getOrganizationById(id);
        organizationRepository.delete(org);
    }
    public List<User> getUsersByOrganizationId(Integer organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        return org.getUsers(); // returns the list of users in that organization
    }


    // Toggle organization status (Active/Inactive)
    public OrganizationDto toggleOrganizationStatus(Integer id) {
        // 1. Get the Organization
        Organization organization = getOrganizationById(id);

        boolean isEnabled;

        // 2. Toggle Organization Status and determine User 'enabled' state
        if (organization.getStatus() == OrganizationStatus.ACTIVE) {
            organization.setStatus(OrganizationStatus.INACTIVE);
            isEnabled = false; // Organization inactive -> Users disabled
        } else {
            organization.setStatus(OrganizationStatus.ACTIVE);
            isEnabled = true; // Organization active -> Users enabled
        }

        // 3. Save the Organization change
        Organization saved = organizationRepository.save(organization);

        // 4. Fetch all users for this organization
        List<User> organizationUsers = userRepository.findByOrganizationId(saved.getId());

        // 5. Update the 'enabled' status for all users
        if (organizationUsers != null && !organizationUsers.isEmpty()) {
            for (User user : organizationUsers) {
                user.setEnabled(isEnabled);
            }
            // Save all updated users to the database
            userRepository.saveAll(organizationUsers);
        }

        // 6. Return DTO
        int usersCount = organizationUsers != null ? organizationUsers.size() : 0;

        return organizationMapper.toOrganizationDto(saved, usersCount);
    }

    public Organization getOrganizationBySlug(String slug) {
        return organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Organization not found with slug: " + slug));
    }

    public List<OrganizationDto> getPublicOrganizations() {
        return organizationRepository.findByStatus(OrganizationStatus.ACTIVE)
                .stream()
                .map(org -> {
                    int usersCount = (int) userRepository.countUsersByOrganization(org.getId());
                    return organizationMapper.toOrganizationDto(org, usersCount);
                })
                .toList();
    }

    public Organization createOrganizationFromRequest(OrganizationRequest request) {
        Organization org = new Organization();
        org.setName(request.getOrganizationName());
        org.setSlug(generateUniqueSlug(request.getDesiredSlug()));
        org.setStatus(OrganizationStatus.ACTIVE);
        org.setCreatedAt(LocalDateTime.now());
        org.setUsersCount(0);
        org.setProcessesCount(0);
        org.setEmail(request.getContactEmail());
        org.setPhone(request.getPhone());
        org.setAddress(request.getAddress());
        org.setDescription(request.getDescription());
        org.setOrganizationLogoUrl(request.getLogoUrl());

        Organization created = organizationRepository.save(org);

        userService.createInitialAdminForOrganization(created, request);

        return created;
    }

    public String generateUniqueSlug(String desiredSlug) {
        String base = desiredSlug.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
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

    public OrganizationDto getOrganizationDtoBySlug(String slug) {
        Organization org = getOrganizationBySlug(slug);
        int usersCount = (int) userRepository.countUsersByOrganization(org.getId());
        return organizationMapper.toOrganizationDto(org, usersCount);
    }

}

