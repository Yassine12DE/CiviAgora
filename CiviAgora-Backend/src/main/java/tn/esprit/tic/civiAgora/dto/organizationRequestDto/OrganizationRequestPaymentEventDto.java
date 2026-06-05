package tn.esprit.tic.civiAgora.dto.organizationRequestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.PaymentStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequestPaymentEventDto {
    private Integer organizationRequestId;
    private String organizationName;
    private String desiredSlug;
    private String paymentToken;
    private OrganizationRequestStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
    private LocalDateTime activatedAt;
    private String stripeSessionId;
    private LocalDateTime updatedAt;
    private Integer organizationCreatedId;
}
