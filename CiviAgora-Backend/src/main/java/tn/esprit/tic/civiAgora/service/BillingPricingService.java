package tn.esprit.tic.civiAgora.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionBillingCycle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
public class BillingPricingService {

    private final BigDecimal starterMonthly;
    private final BigDecimal starterYearly;
    private final BigDecimal professionalMonthly;
    private final BigDecimal professionalYearly;
    private final BigDecimal enterpriseMonthly;
    private final BigDecimal enterpriseYearly;

    public BillingPricingService(
            @Value("${civox.subscription.pricing.starter.monthly:49.00}") BigDecimal starterMonthly,
            @Value("${civox.subscription.pricing.starter.yearly:499.00}") BigDecimal starterYearly,
            @Value("${civox.subscription.pricing.professional.monthly:99.00}") BigDecimal professionalMonthly,
            @Value("${civox.subscription.pricing.professional.yearly:999.00}") BigDecimal professionalYearly,
            @Value("${civox.subscription.pricing.enterprise.monthly:199.00}") BigDecimal enterpriseMonthly,
            @Value("${civox.subscription.pricing.enterprise.yearly:1999.00}") BigDecimal enterpriseYearly
    ) {
        this.starterMonthly = money(starterMonthly);
        this.starterYearly = money(starterYearly);
        this.professionalMonthly = money(professionalMonthly);
        this.professionalYearly = money(professionalYearly);
        this.enterpriseMonthly = money(enterpriseMonthly);
        this.enterpriseYearly = money(enterpriseYearly);
    }

    public BigDecimal resolveSubscriptionPrice(String planCode, SubscriptionBillingCycle billingCycle) {
        String normalizedPlan = normalize(planCode);
        SubscriptionBillingCycle resolvedBillingCycle = billingCycle == null
                ? SubscriptionBillingCycle.MONTHLY
                : billingCycle;

        return switch (normalizedPlan) {
            case "STARTER" -> resolvedBillingCycle == SubscriptionBillingCycle.YEARLY ? starterYearly : starterMonthly;
            case "PROFESSIONAL" -> resolvedBillingCycle == SubscriptionBillingCycle.YEARLY ? professionalYearly : professionalMonthly;
            case "ENTERPRISE" -> resolvedBillingCycle == SubscriptionBillingCycle.YEARLY ? enterpriseYearly : enterpriseMonthly;
            default -> throw new IllegalArgumentException("Unknown subscription plan: " + planCode);
        };
    }

    public int resolveSubscriptionMonths(SubscriptionBillingCycle billingCycle) {
        return billingCycle == SubscriptionBillingCycle.YEARLY ? 12 : 1;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
