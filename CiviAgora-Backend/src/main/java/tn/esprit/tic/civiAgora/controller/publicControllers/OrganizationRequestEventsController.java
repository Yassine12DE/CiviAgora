package tn.esprit.tic.civiAgora.controller.publicControllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tn.esprit.tic.civiAgora.service.OrganizationRequestPaymentRealtimeService;
import tn.esprit.tic.civiAgora.service.OrganizationRequestService;

@RestController
@RequestMapping("/public/organization-requests")
public class OrganizationRequestEventsController {

    private final OrganizationRequestService organizationRequestService;
    private final OrganizationRequestPaymentRealtimeService realtimeService;

    public OrganizationRequestEventsController(
            OrganizationRequestService organizationRequestService,
            OrganizationRequestPaymentRealtimeService realtimeService
    ) {
        this.organizationRequestService = organizationRequestService;
        this.realtimeService = realtimeService;
    }

    @GetMapping(value = "/{token}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribeToPaymentEvents(@PathVariable("token") String token) {
        organizationRequestService.getPaymentSummaryByToken(token);
        return ResponseEntity.ok(realtimeService.subscribeToPaymentToken(token));
    }
}
