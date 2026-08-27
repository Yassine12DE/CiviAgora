package tn.esprit.tic.civiAgora.dao.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.tic.civiAgora.dao.entity.enums.SurveyResultVisibility;
import tn.esprit.tic.civiAgora.dao.entity.enums.SurveyStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organization_surveys", indexes = {
        @Index(name = "idx_survey_org_status", columnList = "organization_id,status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Survey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(nullable = false, length = 180)
    private String title;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SurveyStatus status;

    private LocalDateTime openingAt;
    private LocalDateTime closingAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SurveyResultVisibility resultVisibility;

    @Column(nullable = false)
    private Boolean featured;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    @Builder.Default
    private List<SurveyQuestion> questions = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void beforeInsert() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = SurveyStatus.DRAFT;
        if (resultVisibility == null) resultVisibility = SurveyResultVisibility.AFTER_CLOSE;
        if (featured == null) featured = false;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
