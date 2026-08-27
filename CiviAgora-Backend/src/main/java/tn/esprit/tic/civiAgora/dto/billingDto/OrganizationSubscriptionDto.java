package tn.esprit.tic.civiAgora.dto.billingDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionBillingCycle;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSubscriptionDto {
    private Integer organizationId;
    private String organizationName;
    private String organizationSlug;
    private String planCode;
    private SubscriptionBillingCycle billingCycle;
    private SubscriptionStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime lastRenewedAt;
    private LocalDateTime pendingSince;
    private Boolean autoRenew;
    private Integer renewalCount;
    private Long remainingDays;
    private Long totalDays;
    private boolean expired;
    private boolean expiringSoon;
}
