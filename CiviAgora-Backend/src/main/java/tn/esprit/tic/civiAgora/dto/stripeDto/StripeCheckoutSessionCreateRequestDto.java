package tn.esprit.tic.civiAgora.dto.stripeDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.StripeCheckoutFlow;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeCheckoutSessionCreateRequestDto {

    @NotNull(message = "Checkout flow is required")
    private StripeCheckoutFlow flowType;

    @Size(max = 255, message = "Payment token must be 255 characters or less")
    private String paymentToken;

    private Integer organizationRequestId;

    private Integer organizationId;

    @Size(max = 120, message = "Plan code must be 120 characters or less")
    private String planCode;

    @Size(max = 255, message = "Customer email must be 255 characters or less")
    private String customerEmail;

    @Size(max = 255, message = "Customer name must be 255 characters or less")
    private String customerName;
}
