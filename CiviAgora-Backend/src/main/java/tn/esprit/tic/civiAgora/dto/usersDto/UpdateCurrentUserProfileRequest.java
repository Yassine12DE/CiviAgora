package tn.esprit.tic.civiAgora.dto.usersDto;

import lombok.Data;

@Data
public class UpdateCurrentUserProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String birthDate;
}
