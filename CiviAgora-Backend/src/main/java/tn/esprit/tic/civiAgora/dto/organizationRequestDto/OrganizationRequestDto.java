package tn.esprit.tic.civiAgora.dto.organizationRequestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.PaymentStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequestDto {
    private Integer id;
    private String organizationName;
    private String desiredSlug;
    private String contactPersonName;
    private String contactEmail;
    private String phone;
    private String address;
    private String description;
    private String logoUrl;
    private String adminFirstName;
    private String adminLastName;
    private String adminEmail;
    private Integer expectedNumberOfUsers;
    private List<String> requestedModuleCodes;
    private String requestedPrimaryColor;
    private String requestedSecondaryColor;
    private String brandingNotes;
    private String additionalNotes;
    private OrganizationRequestStatus requestStatus;
    private QuoteStatus quoteStatus;
    private PaymentStatus paymentStatus;
    private Boolean publicVisibilityRequested;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private String reviewedBy;
    private String declineReason;
    private String approvalNotes;
    private String processedBy;
    private LocalDateTime quoteSentAt;
    private LocalDateTime approvedAt;
    private LocalDateTime declinedAt;
    private LocalDateTime paidAt;
    private LocalDateTime activatedAt;
    private BigDecimal quoteBaseFee;
    private BigDecimal quoteUserFee;
    private BigDecimal quoteModuleFee;
    private BigDecimal quoteSetupFee;
    private BigDecimal quoteTotal;
    private String quoteAssumptions;
    private String quoteAxesSnapshot;
    private Integer organizationCreatedId;
    private String paymentUrl;
    private String emailDeliveryWarning;
}
