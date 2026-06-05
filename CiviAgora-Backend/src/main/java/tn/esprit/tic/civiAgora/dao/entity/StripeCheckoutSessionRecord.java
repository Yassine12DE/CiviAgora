package tn.esprit.tic.civiAgora.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutFlow;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stripe_checkout_sessions")
public class StripeCheckoutSessionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "stripe_session_id", nullable = false, unique = true, length = 255)
    private String stripeSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow_type", nullable = false, length = 64)
    private StripeCheckoutFlow flowType;

    @Column(name = "reference_token", length = 255)
    private String referenceToken;

    @Column(name = "organization_request_id")
    private Integer organizationRequestId;

    @Column(name = "organization_id")
    private Integer organizationId;

    @Column(name = "organization_name", length = 160)
    private String organizationName;

    @Column(name = "organization_slug", length = 160)
    private String organizationSlug;

    @Column(name = "plan_code", length = 120)
    private String planCode;

    @Column(name = "module_summary", length = 512)
    private String moduleSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    private StripeCheckoutStatus paymentStatus;

    @Column(name = "currency", length = 12)
    private String currency;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "checkout_url", length = 2048)
    private String checkoutUrl;

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;
}
