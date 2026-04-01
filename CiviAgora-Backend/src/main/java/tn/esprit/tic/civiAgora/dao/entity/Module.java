package tn.esprit.tic.civiAgora.dao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;
    @Column(nullable = false, unique = true)
    private String code; // VOTE, CONFERENCE, YOUTHSPACE

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private Boolean active = true;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrganizationModule> organizationModules = new ArrayList<>();
}
