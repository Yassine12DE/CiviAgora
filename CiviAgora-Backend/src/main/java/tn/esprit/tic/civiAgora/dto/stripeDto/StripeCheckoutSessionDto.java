package tn.esprit.tic.civiAgora.dto.stripeDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutFlow;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionBillingCycle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeCheckoutSessionDto {
    private Integer id;
    private String stripeSessionId;
    private StripeCheckoutFlow flowType;
    private String referenceToken;
    private Integer organizationRequestId;
    private Integer organizationId;
    private String organizationName;
    private String organizationSlug;
    private String planCode;
    private String moduleCode;
    private SubscriptionBillingCycle billingCycle;
    private String subscriptionAction;
    private String moduleSummary;
    private StripeCheckoutStatus paymentStatus;
    private String currency;
    private BigDecimal amount;
    private String customerEmail;
    private String customerName;
    private String checkoutUrl;
    private String stripePaymentIntentId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
