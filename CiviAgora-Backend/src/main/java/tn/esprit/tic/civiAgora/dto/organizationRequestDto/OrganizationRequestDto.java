package tn.esprit.tic.civiAgora.dto.organizationRequestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequestDto {
    private Integer id;
    private String organizationName;
    private String desiredSlug;
    private String contactPersonName;
    private String contactEmail;
    private String phone;
    private String address;
    private String description;
    private String logoUrl;
    private OrganizationRequestStatus requestStatus;
    private Boolean publicVisibilityRequested;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private String reviewedBy;
}
