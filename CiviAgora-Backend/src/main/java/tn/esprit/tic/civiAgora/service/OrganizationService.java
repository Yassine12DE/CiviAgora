package tn.esprit.tic.civiAgora.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
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


    public List<OrganizationDto> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(org -> {
                    int usersCount = (int) userRepository.countUsersByOrganization(org.getId());
                    // Pass count to mapper
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

        Organization organization = getOrganizationById(id);

        if (organization.getStatus() == OrganizationStatus.ACTIVE) {
            organization.setStatus(OrganizationStatus.INACTIVE);
        } else {
            organization.setStatus(OrganizationStatus.ACTIVE);
        }

        Organization saved = organizationRepository.save(organization);

        int usersCount = (int) userRepository.countUsersByOrganization(saved.getId());

        return organizationMapper.toOrganizationDto(saved, usersCount);
    }

}
