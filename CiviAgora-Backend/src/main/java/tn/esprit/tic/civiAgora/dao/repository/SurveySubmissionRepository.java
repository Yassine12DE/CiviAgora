package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.tic.civiAgora.dao.entity.SurveySubmission;

import java.util.List;
import java.util.Optional;

public interface SurveySubmissionRepository extends JpaRepository<SurveySubmission, Long> {
    Optional<SurveySubmission> findBySurveyIdAndUserId(Long surveyId, Integer userId);
    List<SurveySubmission> findBySurveyIdOrderBySubmittedAtAsc(Long surveyId);
    long countBySurveyId(Long surveyId);
    List<SurveySubmission> findByOrganizationId(Integer organizationId);
}
