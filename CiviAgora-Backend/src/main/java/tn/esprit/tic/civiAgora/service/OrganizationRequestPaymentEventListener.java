package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.esprit.tic.civiAgora.event.OrganizationRequestPaymentConfirmedEvent;

@Component
@RequiredArgsConstructor
public class OrganizationRequestPaymentEventListener {

    private final OrganizationRequestPaymentRealtimeService realtimeService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(OrganizationRequestPaymentConfirmedEvent event) {
        if (event != null) {
            realtimeService.publish(event.payload());
        }
    }
}
