package tn.esprit.tic.civiAgora.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationContentType;

import java.time.LocalDateTime;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_org_content_response_user_item",
                columnNames = {"organization_id", "content_item_id", "user_id"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationContentResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_item_id", nullable = false)
    private OrganizationContentItem contentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationContentType type;

    @Column(length = 500)
    private String answer;

    private Boolean participating;

    @Column(length = 100)
    private String reaction;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    private void beforeInsert() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
