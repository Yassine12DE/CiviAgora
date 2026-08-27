package tn.esprit.tic.civiAgora.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionBillingCycle;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String slug;

    @Enumerated(EnumType.STRING)
    private OrganizationStatus status;

    private LocalDateTime createdAt;

    private int usersCount;

    private int processesCount;

    private String email;

    private String phone;

    private String address;

    private String description;
    private String OrganizationLogoUrl;

    private String subscriptionPlanCode;

    @Enumerated(EnumType.STRING)
    private SubscriptionBillingCycle subscriptionBillingCycle;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus subscriptionStatus;

    private LocalDateTime subscriptionStartAt;
    private LocalDateTime subscriptionEndAt;
    private LocalDateTime subscriptionLastRenewedAt;
    private LocalDateTime subscriptionPendingSince;
    private Boolean subscriptionAutoRenew;
    private Integer subscriptionRenewalCount;


    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("organization")
    private List<User> users = new ArrayList<>();




}
