package tn.esprit.tic.civiAgora.controller.saas;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dto.moduleRequestDto.ModuleRequestDto;
import tn.esprit.tic.civiAgora.service.ModuleRequestService;

import java.util.List;

@RestController
@RequestMapping("/saas/module-requests")
@RequiredArgsConstructor
public class ModuleRequestController {

    private final ModuleRequestService moduleRequestService;
// COMMENT 
    @GetMapping
    public ResponseEntity<List<ModuleRequestDto>> getAllRequests() {
        return ResponseEntity.ok(moduleRequestService.getAllRequests());
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ModuleRequestDto> approveRequest(
            @PathVariable Long requestId,
            @RequestParam(required = false) String comment
    ) {
        return ResponseEntity.ok(moduleRequestService.approveRequest(requestId, comment));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ModuleRequestDto> rejectRequest(
            @PathVariable Long requestId,
            @RequestParam(required = false) String comment
    ) {
        return ResponseEntity.ok(moduleRequestService.rejectRequest(requestId, comment));
    }
}