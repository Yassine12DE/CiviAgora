package tn.esprit.tic.civiAgora.dto.usersDto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
