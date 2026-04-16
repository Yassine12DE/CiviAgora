package tn.esprit.tic.civiAgora.dto.organizationRequestDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.OnboardingEmailType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequestResendEmailDto {
    @NotNull(message = "Email type is required")
    private OnboardingEmailType type;
}
