package tn.esprit.tic.civiAgora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class StripeIntegrationConfig {

    @Value("${civox.stripe.secret-key:}")
    private String secretKey;

    @Value("${civox.stripe.publishable-key:}")
    private String publishableKey;

    @Value("${civox.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${civox.stripe.success-url:http://lvh.me:5173/stripe/success}")
    private String successUrl;

    @Value("${civox.stripe.cancel-url:http://lvh.me:5173/stripe/cancel}")
    private String cancelUrl;

    @Value("${civox.stripe.currency:usd}")
    private String currency;

    public String requireSecretKey() {
        String value = normalize(secretKey);
        if (value.isBlank()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY is not configured");
        }
        if (!value.startsWith("sk_test_")) {
            throw new IllegalStateException("STRIPE_SECRET_KEY must start with sk_test_ in test mode");
        }
        return value;
    }

    public String requireWebhookSecret() {
        String value = normalize(webhookSecret);
        if (value.isBlank()) {
            throw new IllegalStateException("STRIPE_WEBHOOK_SECRET is not configured");
        }
        if (!value.startsWith("whsec_")) {
            throw new IllegalStateException("STRIPE_WEBHOOK_SECRET must start with whsec_");
        }
        return value;
    }

    public String getPublishableKey() {
        String value = normalize(publishableKey);
        if (!value.isBlank() && !value.startsWith("pk_test_")) {
            throw new IllegalStateException("STRIPE_PUBLISHABLE_KEY must start with pk_test_ in test mode");
        }
        return value;
    }

    public String getSuccessUrl() {
        return normalize(successUrl);
    }

    public String getCancelUrl() {
        return normalize(cancelUrl);
    }

    public String getCurrency() {
        return normalize(currency).isBlank() ? "usd" : normalize(currency).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
