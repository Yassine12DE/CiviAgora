package tn.esprit.tic.civiAgora.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organization_requests")
public class OrganizationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String organizationName;

    @Column(nullable = false, unique = true)
    private String desiredSlug;

    @Column(nullable = false)
    private String contactPersonName;

    @Column(nullable = false)
    private String contactEmail;

    private String phone;

    private String address;

    @Column(length = 2048)
    private String description;

    private String logoUrl;

    @Enumerated(EnumType.STRING)
    private OrganizationRequestStatus requestStatus;

    private Boolean publicVisibilityRequested;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @Column(length = 2048)
    private String reviewComment;

    private String reviewedBy;
}
