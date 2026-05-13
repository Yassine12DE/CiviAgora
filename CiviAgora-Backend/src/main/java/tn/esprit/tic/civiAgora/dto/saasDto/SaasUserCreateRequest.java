package tn.esprit.tic.civiAgora.dto.saasDto;

import lombok.Data;

@Data
public class SaasUserCreateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String role;
    private Integer organizationId;
}
