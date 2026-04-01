package tn.esprit.tic.civiAgora.controller.publicControllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.dto.organizationSettingsDto.OrganizationSettingsDto;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;
import tn.esprit.tic.civiAgora.service.OrganizationSettingsService;

import java.util.List;

@RestController
@RequestMapping("/public/organizations")
@RequiredArgsConstructor
public class PublicOrganizationController {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsService organizationSettingsService;
    private final OrganizationModuleService organizationModuleService;

    @GetMapping("/{slug}/settings")
    public ResponseEntity<OrganizationSettingsDto> getOrganizationSettings(@PathVariable String slug) {
        Organization organization = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        return ResponseEntity.ok(
                organizationSettingsService.getSettingsByOrganizationId(organization.getId())
        );
    }

    @GetMapping("/{slug}/modules")
    public ResponseEntity<List<OrganizationModuleDto>> getVisibleModules(@PathVariable String slug) {
        Organization organization = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        return ResponseEntity.ok(
                organizationModuleService.getVisibleModulesForOrganization(organization.getId())
        );
    }
}