package tn.esprit.tic.civiAgora.controller.publicControllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutStatus;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionCreateRequestDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripePaymentIntentDto;
import tn.esprit.tic.civiAgora.service.StripeCheckoutService;
import tn.esprit.tic.civiAgora.service.StripePaymentIntentService;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/public/stripe")
public class StripeCheckoutController {

    private final StripeCheckoutService stripeCheckoutService;
    private final StripePaymentIntentService stripePaymentIntentService;

    public StripeCheckoutController(
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

    @GetMapping("/checkout-sessions/{sessionId}")
    public ResponseEntity<StripeCheckoutSessionDto> getCheckoutSession(
            @PathVariable("sessionId") String sessionId
    ) {
        return ResponseEntity.ok(stripeCheckoutService.getCheckoutSessionByStripeSessionId(sessionId));
    }

    @PostMapping("/checkout-sessions/{sessionId}/refresh")
    public ResponseEntity<StripeCheckoutSessionDto> refreshCheckoutSession(
            @PathVariable("sessionId") String sessionId
    ) {
        return ResponseEntity.ok(stripeCheckoutService.refreshCheckoutSession(sessionId));
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> receiveWebhook(
            HttpServletRequest request
    ) throws Exception {
        String payload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        String signatureHeader = request.getHeader("Stripe-Signature");
        stripeCheckoutService.handleCheckoutSessionCompleted(payload, signatureHeader);
        return ResponseEntity.ok(Map.of("message", "Stripe webhook processed"));
    }
}
