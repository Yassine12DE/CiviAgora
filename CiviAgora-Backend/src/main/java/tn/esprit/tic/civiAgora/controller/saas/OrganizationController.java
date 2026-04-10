package tn.esprit.tic.civiAgora.controller.saas;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dto.organizationDto.OrganizationDto;
import tn.esprit.tic.civiAgora.dto.organizationDto.UserToOrganizationDto;
import tn.esprit.tic.civiAgora.mappers.organizationMappers.UserToOrganizationMapper;
import tn.esprit.tic.civiAgora.service.OrganizationService;
import tn.esprit.tic.civiAgora.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/saas/organizations")
public class OrganizationController {
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserToOrganizationMapper userToOrganizationMapper;

    // Get all organizations
    // ✅ Get all organizations (DTO)
    @GetMapping
    public ResponseEntity<List<OrganizationDto>> getAllOrganizations() {
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }
    // Get organization by ID
    @GetMapping("{id}")
    public ResponseEntity<Organization> getOrganizationById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    // Create a new organization
    @PostMapping
    public ResponseEntity<Organization> createOrganization(@RequestBody Organization organization) {
        Organization created = organizationService.createOrganization(organization);
        return ResponseEntity.ok(created);
    }

    // Update an existing organization
    @PutMapping("{id}")
    public ResponseEntity<Organization> updateOrganization(@PathVariable("id") Integer id,
                                                           @RequestBody Organization organization) {
        return ResponseEntity.ok(organizationService.updateOrganization(id, organization));
    }

    // Delete an organization
    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteOrganization(@PathVariable("id") Integer id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok("Organization deleted successfully");
    }
    // Toggle organization status
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<OrganizationDto> toggleOrganizationStatus(
            @PathVariable("id") Integer id) {

        return ResponseEntity.ok(
                organizationService.toggleOrganizationStatus(id)
        );
    }


    // Add a user to an organization
    @PostMapping("{orgId}/users/{userId}")
    public ResponseEntity<UserToOrganizationDto> addUserToOrganization(
            @PathVariable("orgId") Integer orgId,
            @PathVariable("userId") Integer userId) {

        User user = userService.addUserToOrganization(userId, orgId);
        UserToOrganizationDto response = userToOrganizationMapper.toUserToOrganizationDto(user);

        return ResponseEntity.ok(response);
    }
    @GetMapping("{orgId}/users")
    public ResponseEntity<List<UserToOrganizationDto>> getUsersByOrganization(
            @PathVariable("orgId") Integer orgId) {

        List<User> users = organizationService.getUsersByOrganizationId(orgId);

        // Map each User to DTO
        List<UserToOrganizationDto> dtoList = users.stream()
                .map(userToOrganizationMapper::toUserToOrganizationDto)
                .toList();

        return ResponseEntity.ok(dtoList);
    }

}
