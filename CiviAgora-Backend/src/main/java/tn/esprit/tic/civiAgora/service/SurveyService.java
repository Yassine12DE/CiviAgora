package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.dao.entity.*;
import tn.esprit.tic.civiAgora.dao.entity.enums.*;
import tn.esprit.tic.civiAgora.dao.repository.*;
import tn.esprit.tic.civiAgora.dto.surveyDto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SurveyService {
    private static final String MODULE_CODE = "SURVEYS";

    private final SurveyRepository surveyRepository;
    private final SurveySubmissionRepository submissionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationModuleRepository organizationModuleRepository;
    private final OrganizationSubscriptionAccessPolicy subscriptionAccessPolicy;
    private final TenantAccessService tenantAccessService;

    @Transactional(readOnly = true)
    public List<SurveyDto> listPublic() {
        Organization organization = tenantAccessService.getResolvedOrganizationOrThrow();
        requireModuleEnabled(organization);
        return surveyRepository.findByOrganizationIdAndStatusOrderByFeaturedDescCreatedAtDesc(
                        organization.getId(), SurveyStatus.PUBLISHED)
                .stream().map(survey -> toDto(survey, null, false, false)).toList();
    }

    @Transactional(readOnly = true)
    public SurveyDto getPublic(Long surveyId) {
        Organization organization = tenantAccessService.getResolvedOrganizationOrThrow();
        requireModuleEnabled(organization);
        Survey survey = getSurvey(organization.getId(), surveyId);
        if (survey.getStatus() != SurveyStatus.PUBLISHED) {
            throw new NoSuchElementException("Survey not found");
        }
        return toDto(survey, null, canPublicSeeResults(survey), false);
    }

    @Transactional(readOnly = true)
    public List<SurveyDto> listForUser(Integer organizationId, User actor) {
        requireModuleEnabled(getOrganization(organizationId));
        List<Survey> surveys = canManage(actor)
                ? surveyRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                : surveyRepository.findByOrganizationIdAndStatusOrderByFeaturedDescCreatedAtDesc(
                        organizationId, SurveyStatus.PUBLISHED);
        return surveys.stream().map(survey -> toDto(survey, actor, false, false)).toList();
    }

    @Transactional(readOnly = true)
    public SurveyDto getForUser(Integer organizationId, Long surveyId, User actor) {
        requireModuleEnabled(getOrganization(organizationId));
        Survey survey = getSurvey(organizationId, surveyId);
        if (survey.getStatus() != SurveyStatus.PUBLISHED && !canManage(actor)) {
            throw new NoSuchElementException("Survey not found");
        }
        return toDto(survey, actor, canUserSeeResults(survey, actor), false);
    }

    public SurveyDto create(Integer organizationId, SurveyUpsertRequest request, User actor) {
        Organization organization = getOrganization(organizationId);
        requireModuleEnabled(organization);
        validateRequest(request);
        Survey survey = Survey.builder()
                .organization(organization)
                .createdBy(actor)
                .questions(new ArrayList<>())
                .build();
        applyRequest(survey, request, true);
        return toDto(surveyRepository.save(survey), actor, false, false);
    }

    public SurveyDto update(Integer organizationId, Long surveyId, SurveyUpsertRequest request, User actor) {
        requireModuleEnabled(getOrganization(organizationId));
        validateRequest(request);
        Survey survey = getSurvey(organizationId, surveyId);
        long responseCount = submissionRepository.countBySurveyId(surveyId);
        if (responseCount > 0 && !questionsMatch(survey, request.getQuestions())) {
            throw new IllegalStateException("Questions cannot be changed after responses have been received");
        }
        applyRequest(survey, request, responseCount == 0);
        return toDto(surveyRepository.save(survey), actor, false, false);
    }

    public SurveyDto submit(Integer organizationId, Long surveyId, SurveySubmissionRequest request, User actor) {
        Organization organization = getOrganization(organizationId);
        requireModuleEnabled(organization);
        Survey survey = getSurvey(organizationId, surveyId);
        if (!isAcceptingResponses(survey)) {
            throw new IllegalStateException("This survey is not accepting responses");
        }

        Map<Long, SurveyAnswerRequest> requestedAnswers = request.getAnswers().stream()
                .collect(Collectors.toMap(SurveyAnswerRequest::getQuestionId, Function.identity(), (a, b) -> {
                    throw new IllegalArgumentException("A question can only be answered once");
                }));

        SurveySubmission submission = submissionRepository.findBySurveyIdAndUserId(surveyId, actor.getId())
                .orElseGet(() -> SurveySubmission.builder()
                        .organization(organization).survey(survey).user(actor).answers(new ArrayList<>()).build());
        Map<Long, SurveyAnswer> existingAnswers = submission.getAnswers().stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), Function.identity()));
        Set<Long> retainedQuestionIds = new HashSet<>();

        for (SurveyQuestion question : survey.getQuestions()) {
            SurveyAnswerRequest answerRequest = requestedAnswers.remove(question.getId());
            List<String> values = answerRequest == null ? List.of() : cleanValues(answerRequest.getValues());
            if (Boolean.TRUE.equals(question.getRequired()) && values.isEmpty()) {
                throw new IllegalArgumentException("A response is required for: " + question.getPrompt());
            }
            if (!values.isEmpty()) {
                validateAnswer(question, values);
                retainedQuestionIds.add(question.getId());
                SurveyAnswer answer = existingAnswers.get(question.getId());
                if (answer == null) {
                    answer = SurveyAnswer.builder().submission(submission).question(question).build();
                    submission.getAnswers().add(answer);
                }
                answer.setValueText(String.join("\n", values));
            }
        }
        submission.getAnswers().removeIf(answer -> !retainedQuestionIds.contains(answer.getQuestion().getId()));
        if (!requestedAnswers.isEmpty()) {
            throw new IllegalArgumentException("One or more answers do not belong to this survey");
        }

        submissionRepository.save(submission);
        return toDto(survey, actor, canUserSeeResults(survey, actor), false);
    }

    @Transactional(readOnly = true)
    public SurveyDto getResults(Integer organizationId, Long surveyId, User actor) {
        requireModuleEnabled(getOrganization(organizationId));
        return toDto(getSurvey(organizationId, surveyId), actor, true, true);
    }

    @Transactional(readOnly = true)
    public String exportCsv(Integer organizationId, Long surveyId) {
        requireModuleEnabled(getOrganization(organizationId));
        Survey survey = getSurvey(organizationId, surveyId);
        List<SurveySubmission> submissions = submissionRepository.findBySurveyIdOrderBySubmittedAtAsc(surveyId);
        StringBuilder csv = new StringBuilder("submittedAt,userId,userEmail");
        for (SurveyQuestion question : survey.getQuestions()) csv.append(',').append(csv(question.getPrompt()));
        csv.append('\n');
        for (SurveySubmission submission : submissions) {
            Map<Long, String> answers = submission.getAnswers().stream().collect(Collectors.toMap(
                    answer -> answer.getQuestion().getId(), SurveyAnswer::getValueText));
            csv.append(csv(format(submission.getSubmittedAt()))).append(',')
                    .append(submission.getUser().getId()).append(',')
                    .append(csv(submission.getUser().getEmail()));
            for (SurveyQuestion question : survey.getQuestions()) {
                csv.append(',').append(csv(answers.getOrDefault(question.getId(), "")));
            }
            csv.append('\n');
        }
        return csv.toString();
    }

    private void applyRequest(Survey survey, SurveyUpsertRequest request, boolean replaceQuestions) {
        survey.setTitle(request.getTitle().trim());
        survey.setDescription(trimToNull(request.getDescription()));
        survey.setStatus(request.getStatus() == null ? SurveyStatus.DRAFT : request.getStatus());
        survey.setOpeningAt(request.getOpeningAt());
        survey.setClosingAt(request.getClosingAt());
        survey.setResultVisibility(request.getResultVisibility() == null
                ? SurveyResultVisibility.AFTER_CLOSE : request.getResultVisibility());
        survey.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        if (replaceQuestions) {
            survey.getQuestions().clear();
            List<SurveyQuestionRequest> questions = request.getQuestions() == null ? List.of() : request.getQuestions();
            for (int index = 0; index < questions.size(); index++) {
                SurveyQuestionRequest source = questions.get(index);
                survey.getQuestions().add(SurveyQuestion.builder()
                        .survey(survey).position(index).prompt(source.getPrompt().trim()).type(source.getType())
                        .required(Boolean.TRUE.equals(source.getRequired()))
                        .optionsText(String.join("\n", cleanValues(source.getOptions()))).build());
            }
        }
    }

    private void validateRequest(SurveyUpsertRequest request) {
        if (request.getOpeningAt() != null && request.getClosingAt() != null
                && !request.getClosingAt().isAfter(request.getOpeningAt())) {
            throw new IllegalArgumentException("Closing time must be after opening time");
        }
        List<SurveyQuestionRequest> questions = request.getQuestions() == null ? List.of() : request.getQuestions();
        if (request.getStatus() == SurveyStatus.PUBLISHED && questions.isEmpty()) {
            throw new IllegalArgumentException("A published survey needs at least one question");
        }
        for (SurveyQuestionRequest question : questions) {
            if ((question.getType() == SurveyQuestionType.SINGLE_CHOICE
                    || question.getType() == SurveyQuestionType.MULTIPLE_CHOICE)
                    && cleanValues(question.getOptions()).size() < 2) {
                throw new IllegalArgumentException("Choice questions need at least two options");
            }
        }
    }

    private boolean questionsMatch(Survey survey, List<SurveyQuestionRequest> requested) {
        if (requested == null || survey.getQuestions().size() != requested.size()) return false;
        for (int index = 0; index < requested.size(); index++) {
            SurveyQuestion current = survey.getQuestions().get(index);
            SurveyQuestionRequest candidate = requested.get(index);
            if (!Objects.equals(current.getPrompt(), candidate.getPrompt() == null ? null : candidate.getPrompt().trim())
                    || current.getType() != candidate.getType()
                    || !Objects.equals(Boolean.TRUE.equals(current.getRequired()), Boolean.TRUE.equals(candidate.getRequired()))
                    || !Objects.equals(lines(current.getOptionsText()), cleanValues(candidate.getOptions()))) {
                return false;
            }
        }
        return true;
    }

    private void validateAnswer(SurveyQuestion question, List<String> values) {
        if (question.getType() != SurveyQuestionType.MULTIPLE_CHOICE && values.size() > 1) {
            throw new IllegalArgumentException("Only one value is allowed for: " + question.getPrompt());
        }
        List<String> options = lines(question.getOptionsText());
        if (question.getType() == SurveyQuestionType.SINGLE_CHOICE
                || question.getType() == SurveyQuestionType.MULTIPLE_CHOICE) {
            for (String value : values) {
                if (options.stream().noneMatch(option -> option.equalsIgnoreCase(value))) {
                    throw new IllegalArgumentException("Invalid option for: " + question.getPrompt());
                }
            }
        } else if (question.getType() == SurveyQuestionType.YES_NO
                && values.stream().anyMatch(value -> !value.equalsIgnoreCase("yes") && !value.equalsIgnoreCase("no"))) {
            throw new IllegalArgumentException("Answer must be yes or no for: " + question.getPrompt());
        } else if (question.getType() == SurveyQuestionType.RATING) {
            try {
                int rating = Integer.parseInt(values.get(0));
                if (rating < 1 || rating > 5) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Rating must be between 1 and 5 for: " + question.getPrompt());
            }
        } else if (question.getType() == SurveyQuestionType.NUMBER) {
            try { Double.parseDouble(values.get(0)); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("A number is required for: " + question.getPrompt()); }
        } else if (question.getType() == SurveyQuestionType.DATE) {
            try { LocalDate.parse(values.get(0)); }
            catch (Exception ex) { throw new IllegalArgumentException("A valid date is required for: " + question.getPrompt()); }
        }
    }

    private SurveyDto toDto(Survey survey, User actor, boolean includeResults, boolean includeTextResults) {
        SurveySubmission mine = actor == null || actor.getId() == null ? null
                : submissionRepository.findBySurveyIdAndUserId(survey.getId(), actor.getId()).orElse(null);
        Map<Long, List<String>> myAnswers = mine == null ? Map.of() : mine.getAnswers().stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> lines(answer.getValueText())));
        List<SurveySubmission> submissions = includeResults
                ? submissionRepository.findBySurveyIdOrderBySubmittedAtAsc(survey.getId()) : List.of();

        List<SurveyQuestionDto> questions = survey.getQuestions().stream().map(question -> {
            Map<String, Long> counts = includeResults ? aggregate(question, submissions) : Map.of();
            List<String> textResults = includeResults && includeTextResults && isText(question.getType())
                    ? submissions.stream().flatMap(s -> s.getAnswers().stream())
                    .filter(a -> a.getQuestion().getId().equals(question.getId()))
                    .map(SurveyAnswer::getValueText).filter(value -> !value.isBlank()).toList()
                    : List.of();
            return SurveyQuestionDto.builder().id(question.getId()).position(question.getPosition())
                    .prompt(question.getPrompt()).type(question.getType().name()).required(question.getRequired())
                    .options(lines(question.getOptionsText())).myValues(myAnswers.getOrDefault(question.getId(), List.of()))
                    .resultCounts(counts).textResults(textResults).build();
        }).toList();

        User creator = survey.getCreatedBy();
        return SurveyDto.builder().id(survey.getId()).organizationId(survey.getOrganization().getId())
                .title(survey.getTitle()).description(survey.getDescription()).status(survey.getStatus().name())
                .lifecycle(lifecycle(survey)).openingAt(format(survey.getOpeningAt())).closingAt(format(survey.getClosingAt()))
                .resultVisibility(survey.getResultVisibility().name()).featured(survey.getFeatured())
                .createdAt(format(survey.getCreatedAt())).updatedAt(format(survey.getUpdatedAt()))
                .createdByUserId(creator == null ? null : creator.getId()).createdByName(userName(creator))
                .responseCount(submissionRepository.countBySurveyId(survey.getId())).submittedByMe(mine != null)
                .submittedAtByMe(mine == null ? null : format(mine.getSubmittedAt()))
                .acceptingResponses(isAcceptingResponses(survey)).resultsVisible(includeResults).questions(questions).build();
    }

    private Map<String, Long> aggregate(SurveyQuestion question, List<SurveySubmission> submissions) {
        if (isText(question.getType())) return Map.of();
        Map<String, Long> counts = new LinkedHashMap<>();
        if (question.getType() == SurveyQuestionType.SINGLE_CHOICE || question.getType() == SurveyQuestionType.MULTIPLE_CHOICE) {
            lines(question.getOptionsText()).forEach(option -> counts.put(option, 0L));
        }
        for (SurveySubmission submission : submissions) {
            submission.getAnswers().stream().filter(a -> a.getQuestion().getId().equals(question.getId()))
                    .flatMap(a -> lines(a.getValueText()).stream()).forEach(value -> counts.merge(value, 1L, Long::sum));
        }
        return counts;
    }

    private boolean canUserSeeResults(Survey survey, User actor) {
        if (canManage(actor)) return true;
        if (survey.getResultVisibility() == SurveyResultVisibility.PRIVATE) return false;
        if (survey.getResultVisibility() == SurveyResultVisibility.AFTER_CLOSE) return isClosed(survey);
        return actor != null && submissionRepository.findBySurveyIdAndUserId(survey.getId(), actor.getId()).isPresent();
    }

    private boolean canPublicSeeResults(Survey survey) {
        return survey.getResultVisibility() == SurveyResultVisibility.AFTER_CLOSE && isClosed(survey);
    }

    private boolean isAcceptingResponses(Survey survey) {
        LocalDateTime now = LocalDateTime.now();
        return survey.getStatus() == SurveyStatus.PUBLISHED
                && (survey.getOpeningAt() == null || !now.isBefore(survey.getOpeningAt()))
                && (survey.getClosingAt() == null || now.isBefore(survey.getClosingAt()));
    }

    private boolean isClosed(Survey survey) {
        return survey.getClosingAt() != null && !LocalDateTime.now().isBefore(survey.getClosingAt());
    }

    private String lifecycle(Survey survey) {
        if (survey.getStatus() != SurveyStatus.PUBLISHED) return survey.getStatus().name();
        if (survey.getOpeningAt() != null && LocalDateTime.now().isBefore(survey.getOpeningAt())) return "SCHEDULED";
        if (isClosed(survey)) return "CLOSED";
        return "OPEN";
    }

    private boolean canManage(User actor) {
        return actor != null && (actor.getRole() == Role.SUPER_ADMIN || actor.getRole() == Role.ADMIN
                || actor.getRole() == Role.MANAGER);
    }

    private boolean isText(SurveyQuestionType type) {
        return type == SurveyQuestionType.SHORT_TEXT || type == SurveyQuestionType.LONG_TEXT;
    }

    private void requireModuleEnabled(Organization organization) {
        if (!subscriptionAccessPolicy.hasActiveAccess(organization)) {
            throw new AccessDeniedException("This organization subscription is inactive");
        }
        OrganizationModule grant = organizationModuleRepository.findByOrganizationIdAndModuleCode(
                organization.getId(), MODULE_CODE).orElseThrow(() -> new AccessDeniedException("Surveys is not granted"));
        if (grant.getModule() == null || !Boolean.TRUE.equals(grant.getModule().getActive())
                || !Boolean.TRUE.equals(grant.getGrantedBySaas()) || !Boolean.TRUE.equals(grant.getEnabledByOrganization())) {
            throw new AccessDeniedException("Surveys is not enabled for this organization");
        }
    }

    private Organization getOrganization(Integer organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));
    }

    private Survey getSurvey(Integer organizationId, Long surveyId) {
        return surveyRepository.findByIdAndOrganizationId(surveyId, organizationId)
                .orElseThrow(() -> new NoSuchElementException("Survey not found"));
    }

    private List<String> cleanValues(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
    }

    private List<String> lines(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\R")).map(String::trim).filter(v -> !v.isBlank()).toList();
    }

    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String format(LocalDateTime value) { return value == null ? null : value.format(DateTimeFormatter.ISO_DATE_TIME); }
    private String userName(User user) {
        if (user == null) return null;
        String value = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return value.isBlank() ? user.getEmail() : value;
    }
    private String csv(String value) { return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\""; }
}
