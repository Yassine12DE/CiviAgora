package tn.esprit.tic.civiAgora.dao.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "organization_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String homeTitle;

    @Column(length = 2000)
    private String welcomeText;

    private String bannerImageUrl;

    @Column(length = 1000)
    private String footerText;
}