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
import tn.esprit.tic.civiAgora.service.RbacService;

import java.util.List;

@RestController
@RequestMapping("/org/{organizationId}")
@RequiredArgsConstructor
public class OrganizationBackOfficeController {

    private final OrganizationModuleService organizationModuleService;
    private final OrganizationSettingsService organizationSettingsService;
    private final ModuleRequestService moduleRequestService;
    private final RbacService rbacService;

    @GetMapping("/modules")
    public ResponseEntity<List<OrganizationModuleDto>> getGrantedModules(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantBackOfficeAccess(organizationId);
        return ResponseEntity.ok(organizationModuleService.getTenantModules(organizationId));
    }

    @PatchMapping("/modules/{moduleCode}/visibility")
    public ResponseEntity<OrganizationModuleDto> updateModuleVisibility(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("moduleCode") String moduleCode,
            @RequestParam("enabled") Boolean enabled
    ) {
        rbacService.requireTenantModuleVisibilityAccess(organizationId);
        return ResponseEntity.ok(
                organizationModuleService.updateTenantModuleVisibilityForOrganization(
                        organizationId,
                        moduleCode,
                        enabled
                )
        );
    }

    @GetMapping("/settings")
    public ResponseEntity<OrganizationSettingsDto> getSettings(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantDesignCustomizationAccess(organizationId);
        return ResponseEntity.ok(organizationSettingsService.getTenantSettings(organizationId));
    }

    @PutMapping("/settings")
    public ResponseEntity<OrganizationSettingsDto> updateSettings(
            @PathVariable("organizationId") Integer organizationId,
            @RequestBody OrganizationSettingsDto settings
    ) {
        rbacService.requireTenantDesignCustomizationAccess(organizationId);
        return ResponseEntity.ok(organizationSettingsService.updateTenantSettings(organizationId, settings));
    }

    @PostMapping("/module-requests/{moduleCode}")
    public ResponseEntity<ModuleRequestDto> createModuleRequest(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("moduleCode") String moduleCode,
            @RequestParam(value = "comment", required = false) String comment
    ) {
        rbacService.requireTenantModuleRequestAccess(organizationId);
        return ResponseEntity.ok(
                moduleRequestService.createTenantRequest(organizationId, moduleCode, comment)
        );
    }

    @GetMapping("/module-requests")
    public ResponseEntity<List<ModuleRequestDto>> getOrganizationRequests(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantModuleRequestAccess(organizationId);
        return ResponseEntity.ok(moduleRequestService.getTenantRequestsByOrganization(organizationId));
    }
}
