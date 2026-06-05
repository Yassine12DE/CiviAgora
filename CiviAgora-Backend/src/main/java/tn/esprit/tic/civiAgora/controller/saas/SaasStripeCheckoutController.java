package tn.esprit.tic.civiAgora.controller.saas;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionCreateRequestDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionDto;
import tn.esprit.tic.civiAgora.service.StripeCheckoutService;

@RestController
@RequestMapping("/saas/stripe")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SaasStripeCheckoutController {

    private final StripeCheckoutService stripeCheckoutService;

    public SaasStripeCheckoutController(StripeCheckoutService stripeCheckoutService) {
        this.stripeCheckoutService = stripeCheckoutService;
    }

    @PostMapping("/checkout-sessions")
    public ResponseEntity<StripeCheckoutSessionDto> createCheckoutSession(
            @Valid @RequestBody StripeCheckoutSessionCreateRequestDto request
    ) {
        return ResponseEntity.ok(stripeCheckoutService.createCheckoutSession(request));
    }
}
