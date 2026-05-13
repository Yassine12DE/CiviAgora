package tn.esprit.tic.civiAgora.mappers.usersMapper;

import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dto.usersDto.BackOfficeSAASUserDto;

import java.time.format.DateTimeFormatter;

public class BackOfficeSAASUserMapper {

    public static BackOfficeSAASUserDto toBackOfficeSAASUserDto(User user) {

        if (user == null) {
            return null;
        }

        return BackOfficeSAASUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
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
                .archived(Boolean.TRUE.equals(user.getArchived()))
                .build();
    }
}
