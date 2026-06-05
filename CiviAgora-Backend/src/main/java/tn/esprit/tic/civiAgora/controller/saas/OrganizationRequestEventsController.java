package tn.esprit.tic.civiAgora.controller.saas;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tn.esprit.tic.civiAgora.service.OrganizationRequestPaymentRealtimeService;

@RestController
@RequestMapping("/saas/organization-requests")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class OrganizationRequestEventsController {

    private final OrganizationRequestPaymentRealtimeService realtimeService;

    public OrganizationRequestEventsController(OrganizationRequestPaymentRealtimeService realtimeService) {
        this.realtimeService = realtimeService;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribeToPaymentEvents() {
        return ResponseEntity.ok(realtimeService.subscribeToBackOffice());
    }
}
