package tn.esprit.tic.civiAgora.mappers.organizationMappers;

import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dto.organizationDto.UserToOrganizationDto;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


@Component
public class UserToOrganizationMapper {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ISO_INSTANT;
    public UserToOrganizationDto toUserToOrganizationDto(User user) {
        return UserToOrganizationDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(Boolean.TRUE.equals(user.getEnabled()) ? "active" : "inactive")
                .createdAt(user.getCreatedTimestamp() != null
                        ? user.getCreatedTimestamp()
                        .toInstant()
                        .atZone(ZoneOffset.UTC)
                        .format(DATE_TIME_FORMAT)
                        : null)
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .build();
    }
}
