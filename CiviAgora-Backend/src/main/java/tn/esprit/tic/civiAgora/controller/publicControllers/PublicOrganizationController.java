package tn.esprit.tic.civiAgora.controller.publicControllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.dto.organizationSettingsDto.OrganizationSettingsDto;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;
import tn.esprit.tic.civiAgora.service.OrganizationService;
import tn.esprit.tic.civiAgora.service.OrganizationSettingsService;

import java.util.List;

@RestController
@RequestMapping("/public/organization")
@RequiredArgsConstructor
public class PublicOrganizationController {

    private final OrganizationSettingsService organizationSettingsService;
    private final OrganizationModuleService organizationModuleService;
    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<?> getCurrentOrganization() {
        return ResponseEntity.ok(organizationService.getCurrentOrganizationBranding());
    }

    @GetMapping("/settings")
    public ResponseEntity<OrganizationSettingsDto> getOrganizationSettings() {
        return ResponseEntity.ok(organizationSettingsService.getCurrentOrganizationSettings());
    }

    @GetMapping("/modules")
    public ResponseEntity<List<OrganizationModuleDto>> getVisibleModules() {
        return ResponseEntity.ok(organizationModuleService.getVisibleModulesForCurrentOrganization());
    }
}
