package tn.esprit.tic.civiAgora.dao.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleRequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "module_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModuleRequestStatus status;

    private LocalDateTime requestDate;
    private LocalDateTime reviewedDate;

    @Column(length = 1000)
    private String comment;
}