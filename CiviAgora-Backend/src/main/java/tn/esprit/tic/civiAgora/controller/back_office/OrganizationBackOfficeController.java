package tn.esprit.tic.civiAgora.controller.back_office;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.dto.moduleRequestDto.ModuleRequestDto;
import tn.esprit.tic.civiAgora.dto.organizationSettingsDto.OrganizationSettingsDto;
import tn.esprit.tic.civiAgora.service.ModuleRequestService;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;
import tn.esprit.tic.civiAgora.service.OrganizationSettingsService;

import java.util.List;

@RestController
@RequestMapping("/org/{organizationId}")
@RequiredArgsConstructor
public class OrganizationBackOfficeController {

    private final OrganizationModuleService organizationModuleService;
    private final OrganizationSettingsService organizationSettingsService;
    private final ModuleRequestService moduleRequestService;

    @GetMapping("/modules")
    public ResponseEntity<List<OrganizationModuleDto>> getGrantedModules(@PathVariable Integer organizationId) {
        return ResponseEntity.ok(organizationModuleService.getAllModulesForOrganization(organizationId));
    }

    @PatchMapping("/modules/{moduleCode}/visibility")
    public ResponseEntity<OrganizationModuleDto> updateModuleVisibility(
            @PathVariable Integer organizationId,
            @PathVariable String moduleCode,
            @RequestParam Boolean enabled
    ) {
        return ResponseEntity.ok(
                organizationModuleService.updateModuleVisibilityForOrganization(
                        organizationId,
                        moduleCode,
                        enabled
                )
        );
    }

    @GetMapping("/settings")
    public ResponseEntity<OrganizationSettingsDto> getSettings(@PathVariable Integer organizationId) {
        return ResponseEntity.ok(organizationSettingsService.getSettingsByOrganizationId(organizationId));
    }

    @PutMapping("/settings")
    public ResponseEntity<OrganizationSettingsDto> updateSettings(
            @PathVariable Integer organizationId,
            @RequestBody OrganizationSettingsDto settings
    ) {
        return ResponseEntity.ok(organizationSettingsService.updateSettings(organizationId, settings));
    }

    @PostMapping("/module-requests/{moduleCode}")
    public ResponseEntity<ModuleRequestDto> createModuleRequest(
            @PathVariable Integer organizationId,
            @PathVariable String moduleCode,
            @RequestParam(required = false) String comment
    ) {
        return ResponseEntity.ok(
                moduleRequestService.createRequest(organizationId, moduleCode, comment)
        );
    }

    @GetMapping("/module-requests")
    public ResponseEntity<List<ModuleRequestDto>> getOrganizationRequests(@PathVariable Integer organizationId) {
        return ResponseEntity.ok(moduleRequestService.getRequestsByOrganization(organizationId));
    }
}