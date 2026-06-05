package tn.esprit.tic.civiAgora.controller.saas;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dto.saasDto.SaasModuleCatalogItemDto;
import tn.esprit.tic.civiAgora.dto.saasDto.SaasModuleUpsertRequest;
import tn.esprit.tic.civiAgora.service.ModuleService;

import java.util.List;

@RestController
@RequestMapping("/saas/modules")
@RequiredArgsConstructor
public class SaasModuleCatalogController {

    private final ModuleService moduleService;

    @GetMapping
    public ResponseEntity<List<SaasModuleCatalogItemDto>> getCatalog() {
        return ResponseEntity.ok(moduleService.getSaasCatalog());
    }

    @PostMapping
    public ResponseEntity<Module> createModule(@RequestBody SaasModuleUpsertRequest request) {
        return ResponseEntity.ok(moduleService.createSaasModule(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Module> updateModule(
            @PathVariable("id") Long id,
            @RequestBody SaasModuleUpsertRequest request
    ) {
        return ResponseEntity.ok(moduleService.updateSaasModule(id, request));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Module> updateModuleActive(
            @PathVariable("id") Long id,
            @RequestParam("active") boolean active
    ) {
        SaasModuleUpsertRequest request = new SaasModuleUpsertRequest();
        request.setActive(active);
        return ResponseEntity.ok(moduleService.updateSaasModule(id, request));
    }
}
