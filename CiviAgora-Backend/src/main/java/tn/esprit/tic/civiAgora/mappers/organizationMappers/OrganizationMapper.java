package tn.esprit.tic.civiAgora.mappers.organizationMappers;

import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dto.organizationDto.OrganizationDto;

import java.time.format.DateTimeFormatter;

@Component
public class OrganizationMapper {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public OrganizationDto toOrganizationDto(Organization organization , int usersCount) {
        return OrganizationDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .slug(organization.getSlug())
                .status(organization.getStatus() != null
                        ? organization.getStatus().name().toLowerCase()
                        : null)
                .createdAt(organization.getCreatedAt() != null
                        ? organization.getCreatedAt().format(DATE_FORMAT)
                        : null)
                .usersCount(usersCount) // ✅ use dynamic count
                .processesCount(organization.getProcessesCount())
                .email(organization.getEmail())
                .phone(organization.getPhone())
                .address(organization.getAddress())
                .description(organization.getDescription())
                .build();
    }
}
