package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.tic.civiAgora.dao.entity.Survey;
import tn.esprit.tic.civiAgora.dao.entity.enums.SurveyStatus;

import java.util.List;
import java.util.Optional;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    List<Survey> findByOrganizationIdOrderByCreatedAtDesc(Integer organizationId);
    List<Survey> findByOrganizationIdAndStatusOrderByFeaturedDescCreatedAtDesc(Integer organizationId, SurveyStatus status);
    Optional<Survey> findByIdAndOrganizationId(Long id, Integer organizationId);
    List<Survey> findByOrganizationId(Integer organizationId);
}
