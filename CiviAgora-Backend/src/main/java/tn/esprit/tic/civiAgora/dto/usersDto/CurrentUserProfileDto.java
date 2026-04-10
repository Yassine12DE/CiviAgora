package tn.esprit.tic.civiAgora.dto.usersDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserProfileDto {
    private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String birthDate;
    private String role;
    private Boolean enabled;
    private Boolean archived;
    private String accountStatus;
    private Integer organizationId;
    private String organizationSlug;
    private String organizationName;
}
