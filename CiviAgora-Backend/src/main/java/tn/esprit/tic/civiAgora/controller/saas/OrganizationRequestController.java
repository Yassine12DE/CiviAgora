package tn.esprit.tic.civiAgora.controller.saas;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestDecisionDto;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestDto;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestResendEmailDto;
import tn.esprit.tic.civiAgora.service.OrganizationRequestService;

import java.util.List;

@RestController
@RequestMapping("/saas/organization-requests")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class OrganizationRequestController {

    private final OrganizationRequestService requestService;

    public OrganizationRequestController(OrganizationRequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping
    public ResponseEntity<List<OrganizationRequestDto>> getAllRequests(
            @RequestParam(value = "status", required = false) OrganizationRequestStatus status,
            @RequestParam(value = "search", required = false) String search
    ) {
        return ResponseEntity.ok(requestService.searchRequests(status, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationRequestDto> getRequestById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    @PostMapping("/{id}/quote")
    public ResponseEntity<OrganizationRequestDto> generateAndSendQuote(
            @PathVariable("id") Integer id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(requestService.generateAndSendQuote(id, actorEmail(authentication)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<OrganizationRequestDto> approveRequest(
            @PathVariable("id") Integer id,
            @Valid @RequestBody(required = false) OrganizationRequestDecisionDto decision,
            @RequestParam(value = "reviewComment", required = false) String legacyReviewComment,
            @RequestParam(value = "reviewedBy", required = false) String legacyReviewedBy,
            Authentication authentication
    ) {
        String reviewer = legacyReviewedBy != null ? legacyReviewedBy : actorEmail(authentication);
        String notes = decision != null && decision.getNotes() != null ? decision.getNotes() : legacyReviewComment;
        return ResponseEntity.ok(requestService.approveRequest(id, reviewer, notes));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<OrganizationRequestDto> declineRequest(
            @PathVariable("id") Integer id,
            @Valid @RequestBody(required = false) OrganizationRequestDecisionDto decision,
            Authentication authentication
    ) {
        String reason = decision == null ? null : decision.getReason();
        return ResponseEntity.ok(requestService.declineRequest(id, actorEmail(authentication), reason));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OrganizationRequestDto> rejectRequest(
            @PathVariable("id") Integer id,
            @RequestParam(value = "reviewComment", required = false) String reviewComment,
            @RequestParam(value = "reviewedBy", required = false) String reviewedBy,
            Authentication authentication
    ) {
        return ResponseEntity.ok(requestService.rejectRequest(
                id,
                reviewedBy != null ? reviewedBy : actorEmail(authentication),
                reviewComment
        ));
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<OrganizationRequestDto> markPaymentCompleted(
            @PathVariable("id") Integer id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(requestService.markPaymentCompleted(id, actorEmail(authentication)));
    }

    @PostMapping("/{id}/resend-email")
    public ResponseEntity<OrganizationRequestDto> resendEmail(
            @PathVariable("id") Integer id,
            @Valid @RequestBody OrganizationRequestResendEmailDto resendEmailDto
    ) {
        return ResponseEntity.ok(requestService.resendEmail(id, resendEmailDto.getType()));
    }

    private String actorEmail(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }
        return "SUPER_ADMIN";
    }
}
