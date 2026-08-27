package tn.esprit.tic.civiAgora.dao.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleBillingType;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleScope;

import java.math.BigDecimal;
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

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    @Builder.Default
    private ModuleScope scope = ModuleScope.BOTH;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    @Builder.Default
    private ModuleBillingType billingType = ModuleBillingType.ONE_TIME;

    @Column(precision = 12, scale = 2)
    private BigDecimal oneTimePrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal yearlyPrice;

    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrganizationModule> organizationModules = new ArrayList<>();
}
