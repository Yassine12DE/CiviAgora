package tn.esprit.tic.civiAgora.dto.billingDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleBillingType;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModulePurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModulePurchaseDto {
    private Long id;
    private Integer organizationId;
    private String organizationName;
    private Long moduleId;
    private String moduleCode;
    private String moduleName;
    private ModuleBillingType billingType;
    private ModulePurchaseStatus status;
    private BigDecimal amount;
    private String currency;
    private String stripeSessionId;
    private String stripePaymentIntentId;
    private String customerEmail;
    private String customerName;
    private String comment;
    private LocalDateTime requestedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime activatedAt;
}
