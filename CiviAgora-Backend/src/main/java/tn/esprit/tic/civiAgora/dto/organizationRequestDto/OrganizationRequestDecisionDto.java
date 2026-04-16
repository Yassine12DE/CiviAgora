package tn.esprit.tic.civiAgora.dto.organizationRequestDto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequestDecisionDto {
    @Size(max = 2048, message = "Notes must be 2048 characters or less")
    private String notes;

    @Size(max = 2048, message = "Reason must be 2048 characters or less")
    private String reason;
}
