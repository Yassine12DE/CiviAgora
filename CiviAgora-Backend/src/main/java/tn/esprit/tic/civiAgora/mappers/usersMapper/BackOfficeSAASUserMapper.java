package tn.esprit.tic.civiAgora.mappers.usersMapper;

import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dto.usersDto.BackOfficeSAASUserDto;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;

import java.time.format.DateTimeFormatter;
import java.util.Set;

public class BackOfficeSAASUserMapper {

    private static final Set<Role> ALLOWED_ROLES = Set.of(
            Role.ADMIN,
            Role.MODERATOR,
            Role.MANAGER,
            Role.OBSERVER
    );

    public static BackOfficeSAASUserDto toBackOfficeSAASUserDto(User user) {

        if (user == null || !ALLOWED_ROLES.contains(user.getRole())) {
            return null;
        }

        return BackOfficeSAASUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .organization(
                        user.getOrganization() != null
                                ? user.getOrganization().getName()
                                : null
                )
                // STATUS
                .status(Boolean.TRUE.equals(user.getEnabled()) ? "ACTIVE" : "INACTIVE")

                // CREATED AT
                .createdAt(
                        user.getCreatedTimestamp() != null
                                ? user.getCreatedTimestamp()
                                .toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                : null
                )
                .build();
    }
}
