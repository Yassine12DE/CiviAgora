package tn.esprit.tic.civiAgora.controller.saas;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;

import java.util.List;

@RestController
@RequestMapping("/saas/organizations/{organizationId}/modules")
@RequiredArgsConstructor
public class SaasOrganizationModuleController {

    private final OrganizationModuleService organizationModuleService;

    @GetMapping
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<OrganizationModuleDto>> getOrganizationModules(@PathVariable Integer organizationId) {
        return ResponseEntity.ok(organizationModuleService.getAllModulesForOrganization(organizationId));
    }

    @PostMapping("/{moduleCode}")
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<OrganizationModuleDto> grantModule(
            @PathVariable Integer organizationId,
            @PathVariable String moduleCode,
            @RequestParam(required = false) Integer displayOrder
    ) {
        return ResponseEntity.ok(
                organizationModuleService.grantModuleToOrganization(organizationId, moduleCode, displayOrder)
        );
    }

    @DeleteMapping("/{moduleCode}")
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<String> removeGrantedModule(
            @PathVariable Integer organizationId,
            @PathVariable String moduleCode
    ) {
        organizationModuleService.removeGrantedModule(organizationId, moduleCode);
        return ResponseEntity.ok("Module removed successfully");
    }
}