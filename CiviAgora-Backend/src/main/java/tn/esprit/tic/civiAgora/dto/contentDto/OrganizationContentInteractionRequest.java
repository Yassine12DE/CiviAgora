package tn.esprit.tic.civiAgora.dto.contentDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationContentInteractionRequest {
    private String answer;
    private Boolean participating;
    private String reaction;
}
