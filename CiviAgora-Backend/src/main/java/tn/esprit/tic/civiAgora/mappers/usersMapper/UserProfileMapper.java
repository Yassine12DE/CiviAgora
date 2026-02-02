package tn.esprit.tic.civiAgora.mappers.usersMapper;

import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dto.usersDto.UserProfileDto;

public class UserProfileMapper {
    public static UserProfileDto getUserProfileDto(User user) {
        return UserProfileDto.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}
