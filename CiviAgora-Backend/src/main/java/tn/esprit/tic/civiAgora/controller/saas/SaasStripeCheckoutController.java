package tn.esprit.tic.civiAgora.controller.saas;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionCreateRequestDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripePaymentIntentDto;
import tn.esprit.tic.civiAgora.service.StripeCheckoutService;
import tn.esprit.tic.civiAgora.service.StripePaymentIntentService;

@RestController
@RequestMapping("/saas/stripe")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SaasStripeCheckoutController {

    private final StripeCheckoutService stripeCheckoutService;
    private final StripePaymentIntentService stripePaymentIntentService;

    public SaasStripeCheckoutController(
            StripeCheckoutService stripeCheckoutService,
            StripePaymentIntentService stripePaymentIntentService
    ) {
        this.stripeCheckoutService = stripeCheckoutService;
        this.stripePaymentIntentService = stripePaymentIntentService;
    }

    @PostMapping("/checkout-sessions")
    public ResponseEntity<StripeCheckoutSessionDto> createCheckoutSession(
            @Valid @RequestBody StripeCheckoutSessionCreateRequestDto request
    ) {
        return ResponseEntity.ok(stripeCheckoutService.createCheckoutSession(request));
    }

    @PostMapping("/payment-intents")
    public ResponseEntity<StripePaymentIntentDto> createPaymentIntent(
            @Valid @RequestBody StripeCheckoutSessionCreateRequestDto request
    ) {
        return ResponseEntity.ok(stripePaymentIntentService.createPaymentIntent(request));
    }

    @PostMapping("/payment-intents/{paymentIntentId}/sync")
    public ResponseEntity<StripePaymentIntentDto> syncPaymentIntent(
            @PathVariable("paymentIntentId") String paymentIntentId
    ) {
        return ResponseEntity.ok(stripePaymentIntentService.syncPaymentIntent(paymentIntentId));
    }
}
