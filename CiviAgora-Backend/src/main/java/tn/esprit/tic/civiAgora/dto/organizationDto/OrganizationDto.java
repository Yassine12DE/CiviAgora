package tn.esprit.tic.civiAgora.dto.organizationDto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionBillingCycle;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationDto {
    private Integer id;
    private String name;
    private String slug;
    private String status;
    private String createdAt;   // KEEP AS STRING
    private int usersCount;
    private int processesCount;
    private String email;
    private String phone;
    private String address;
    private String description;
    private String subscriptionPlanCode;
    private SubscriptionBillingCycle subscriptionBillingCycle;
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime subscriptionStartAt;
    private LocalDateTime subscriptionEndAt;
    private LocalDateTime subscriptionLastRenewedAt;
    private LocalDateTime subscriptionPendingSince;
    private Boolean subscriptionAutoRenew;
    private Integer subscriptionRenewalCount;
}
