package tn.esprit.tic.civiAgora.dao.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleBillingType;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModulePurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModulePurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ModulePurchaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", length = 32)
    private ModuleBillingType billingType;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 12)
    private String currency;

    @Column(length = 255, unique = true)
    private String stripeSessionId;

    @Column(length = 255)
    private String stripePaymentIntentId;

    @Column(length = 255)
    private String customerEmail;

    @Column(length = 255)
    private String customerName;

    @Column(length = 1000)
    private String comment;

    private LocalDateTime requestedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime activatedAt;
}
