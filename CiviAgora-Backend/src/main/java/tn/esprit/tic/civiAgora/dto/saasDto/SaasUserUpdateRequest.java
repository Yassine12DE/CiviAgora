package tn.esprit.tic.civiAgora.dto.saasDto;

import lombok.Data;

@Data
public class SaasUserUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private Integer organizationId;
    private Boolean enabled;
}
