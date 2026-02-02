package tn.esprit.tic.civiAgora.dto.organizationDto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserToOrganizationDto {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private String status;
    private String createdAt;
    private Integer organizationId;
    private String organizationName;

}
