package tn.esprit.tic.civiAgora.controller.saas;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<OrganizationModuleDto>> getOrganizationModules(
            @PathVariable("organizationId") Integer organizationId
    ) {
        return ResponseEntity.ok(organizationModuleService.getAllModulesForOrganization(organizationId));
    }

    @PostMapping("/{moduleReference}")
    public ResponseEntity<List<OrganizationModuleDto>> grantModule(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("moduleReference") String moduleReference,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder
    ) {
        return ResponseEntity.ok(
                organizationModuleService.addModuleToOrganization(
                        organizationId,
                        moduleReference,
                        displayOrder
                )
        );
    }

    @DeleteMapping("/{moduleReference}")
    public ResponseEntity<List<OrganizationModuleDto>> removeGrantedModule(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("moduleReference") String moduleReference
    ) {
        return ResponseEntity.ok(
                organizationModuleService.removeModuleFromOrganization(organizationId, moduleReference)
        );
    }
}
