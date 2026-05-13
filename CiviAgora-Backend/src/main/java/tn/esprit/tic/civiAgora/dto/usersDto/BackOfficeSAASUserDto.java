package tn.esprit.tic.civiAgora.dto.usersDto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class BackOfficeSAASUserDto {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Integer organizationId;
    private String organization; // Name of the organization
    private String status;
    private String createdAt;
    private Boolean archived;

}
