package tn.esprit.tic.civiAgora.dto.organizationRequestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryDto {
    private String organizationName;
    private String desiredSlug;
    private String contactEmail;
    private String adminEmail;
    private Integer expectedNumberOfUsers;
    private List<String> requestedModuleCodes;
    private OrganizationRequestStatus requestStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal quoteBaseFee;
    private BigDecimal quoteUserFee;
    private BigDecimal quoteModuleFee;
    private BigDecimal quoteSetupFee;
    private BigDecimal quoteTotal;
    private String quoteAssumptions;
    private Integer organizationCreatedId;
    private String organizationAccessUrl;
    private String emailDeliveryWarning;
}
