package tn.esprit.tic.civiAgora.dao.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "organization_modules",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"organization_id", "module_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Column(nullable = false)
    private Boolean grantedBySaas = true;

    @Column(nullable = false)
    private Boolean enabledByOrganization = true;

    private Integer displayOrder;
}