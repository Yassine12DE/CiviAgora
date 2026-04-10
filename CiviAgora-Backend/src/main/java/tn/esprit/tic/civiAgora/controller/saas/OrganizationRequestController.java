package tn.esprit.tic.civiAgora.controller.saas;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestDto;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.service.OrganizationRequestService;

import java.util.List;

@RestController
@RequestMapping("/saas/organization-requests")
public class OrganizationRequestController {

    @Autowired
    private OrganizationRequestService requestService;

    @GetMapping
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<OrganizationRequestDto>> getAllRequests(
            @RequestParam(value = "status", required = false) OrganizationRequestStatus status
    ) {
        if (status == null) {
            return ResponseEntity.ok(requestService.getAllRequests());
        }
        return ResponseEntity.ok(requestService.getRequestsByStatus(status));
    }

    @GetMapping("/{id}")
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<OrganizationRequestDto> getRequestById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    @PostMapping("/{id}/approve")
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<OrganizationRequestDto> approveRequest(
            @PathVariable("id") Integer id,
            @RequestParam(value = "reviewComment", required = false) String reviewComment,
            @RequestParam(value = "reviewedBy", required = false) String reviewedBy
    ) {
        return ResponseEntity.ok(requestService.approveRequest(id, reviewedBy, reviewComment));
    }

    @PostMapping("/{id}/reject")
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<OrganizationRequestDto> rejectRequest(
            @PathVariable("id") Integer id,
            @RequestParam(value = "reviewComment", required = false) String reviewComment,
            @RequestParam(value = "reviewedBy", required = false) String reviewedBy
    ) {
        return ResponseEntity.ok(requestService.rejectRequest(id, reviewedBy, reviewComment));
    }
}
