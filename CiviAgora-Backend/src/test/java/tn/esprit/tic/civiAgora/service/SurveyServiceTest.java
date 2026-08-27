package tn.esprit.tic.civiAgora.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.tic.civiAgora.dao.entity.*;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.enums.*;
import tn.esprit.tic.civiAgora.dao.repository.*;
import tn.esprit.tic.civiAgora.dto.surveyDto.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {
    @Mock SurveyRepository surveyRepository;
    @Mock SurveySubmissionRepository submissionRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock OrganizationModuleRepository organizationModuleRepository;
    @Spy OrganizationSubscriptionAccessPolicy subscriptionAccessPolicy = new OrganizationSubscriptionAccessPolicy();
    @Mock TenantAccessService tenantAccessService;
    @InjectMocks SurveyService service;

    @Test
    void publicDetail_isStrictlyScopedToResolvedTenant() {
        Organization tenant = organization(9);
        when(tenantAccessService.getResolvedOrganizationOrThrow()).thenReturn(tenant);
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(9, "SURVEYS"))
                .thenReturn(Optional.of(grant(tenant)));
        when(surveyRepository.findByIdAndOrganizationId(44L, 9)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> service.getPublic(44L));
        verify(surveyRepository).findByIdAndOrganizationId(44L, 9);
    }

    @Test
    void submit_rejectsClosedSurveyBeforeWriting() {
        Organization tenant = organization(4);
        Survey survey = survey(tenant);
        survey.setClosingAt(LocalDateTime.now().minusMinutes(1));
        User citizen = User.builder().id(12).role(Role.CITIZEN).build();
        stubTenant(tenant, survey);

        SurveySubmissionRequest request = new SurveySubmissionRequest();
        SurveyAnswerRequest answer = new SurveyAnswerRequest();
        answer.setQuestionId(100L);
        answer.setValues(List.of("Yes"));
        request.setAnswers(List.of(answer));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.submit(4, 8L, request, citizen));
        assertTrue(error.getMessage().contains("not accepting"));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submit_validatesAndPersistsOneMemberSubmission() {
        Organization tenant = organization(4);
        Survey survey = survey(tenant);
        User citizen = User.builder().id(12).role(Role.CITIZEN).build();
        stubTenant(tenant, survey);
        when(submissionRepository.findBySurveyIdAndUserId(8L, 12)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(SurveySubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SurveySubmissionRequest request = new SurveySubmissionRequest();
        SurveyAnswerRequest answer = new SurveyAnswerRequest();
        answer.setQuestionId(100L);
        answer.setValues(List.of("Yes"));
        request.setAnswers(List.of(answer));

        SurveyDto result = service.submit(4, 8L, request, citizen);

        assertNotNull(result);
        verify(submissionRepository).save(argThat(submission ->
                submission.getUser() == citizen && submission.getAnswers().size() == 1
                        && "Yes".equals(submission.getAnswers().get(0).getValueText())));
    }

    private void stubTenant(Organization tenant, Survey survey) {
        when(organizationRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(tenant.getId(), "SURVEYS"))
                .thenReturn(Optional.of(grant(tenant)));
        when(surveyRepository.findByIdAndOrganizationId(8L, tenant.getId())).thenReturn(Optional.of(survey));
    }

    private Survey survey(Organization tenant) {
        Survey survey = Survey.builder().id(8L).organization(tenant).title("Resident experience")
                .status(SurveyStatus.PUBLISHED).resultVisibility(SurveyResultVisibility.AFTER_SUBMISSION)
                .featured(false).questions(new java.util.ArrayList<>()).createdAt(LocalDateTime.now()).build();
        SurveyQuestion question = SurveyQuestion.builder().id(100L).survey(survey).position(0)
                .prompt("Was this helpful?").type(SurveyQuestionType.YES_NO).required(true).build();
        survey.getQuestions().add(question);
        return survey;
    }

    private Organization organization(int id) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setStatus(OrganizationStatus.ACTIVE);
        return organization;
    }

    private OrganizationModule grant(Organization organization) {
        Module module = Module.builder().code("SURVEYS").name("Surveys").active(true).build();
        return OrganizationModule.builder().organization(organization).module(module)
                .grantedBySaas(true).enabledByOrganization(true).build();
    }
}
