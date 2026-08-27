package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dto.surveyDto.SurveyDto;
import tn.esprit.tic.civiAgora.dto.surveyDto.SurveyQuestionDto;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.service.SurveyService;
import tn.esprit.tic.civiAgora.tools.ToolDefinition;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class SurveyTool implements ToolDefinition {
    private final ObjectMapper objectMapper;
    private final SurveyService surveyService;
    private final RbacService rbacService;

    @Override
    public String getName() {
        return "surveys";
    }

    @Override
    public String getDescription() {
        return "Discover tenant-scoped surveys, the current user's response state, survey details, and authorized results. "
                + "Result visibility and the Surveys entitlement are enforced by backend services.";
    }

    @Override
    public String getRequiredModule() {
        return "SURVEYS";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of("type", "string", "enum", List.of(
                                "list_open", "list_unanswered", "list_answered", "search", "get_detail", "get_results"
                        )),
                        "surveyId", Map.of("type", "integer", "description", "Survey id returned by this tool"),
                        "query", Map.of("type", "string", "description", "Title or description search text")
                ),
                "required", List.of("action")
        );
    }

    @Override
    public String execute(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        String action = String.valueOf(input.get("action"));
        if ("get_detail".equals(action) || "get_results".equals(action)) {
            Long surveyId = longValue(input.get("surveyId"));
            if (surveyId == null) {
                return objectMapper.writeValueAsString(Map.of("ok", false, "message", "Choose a survey first."));
            }
            SurveyDto survey;
            if ("get_results".equals(action)) {
                rbacService.requireTenantAnalyticsAccess(context.organizationId());
                survey = surveyService.getResults(context.organizationId(), surveyId, context.user());
            } else {
                survey = surveyService.getForUser(context.organizationId(), surveyId, context.user());
            }
            return objectMapper.writeValueAsString(toToolSurvey(survey, true));
        }

        List<SurveyDto> surveys = surveyService.listForUser(context.organizationId(), context.user());
        Predicate<SurveyDto> filter = switch (action) {
            case "list_open" -> survey -> Boolean.TRUE.equals(survey.getAcceptingResponses());
            case "list_unanswered" -> survey -> Boolean.TRUE.equals(survey.getAcceptingResponses())
                    && !Boolean.TRUE.equals(survey.getSubmittedByMe());
            case "list_answered" -> survey -> Boolean.TRUE.equals(survey.getSubmittedByMe());
            case "search" -> survey -> matches(survey, stringValue(input.get("query")));
            default -> survey -> false;
        };
        List<Map<String, Object>> result = surveys.stream().filter(filter).map(survey -> toToolSurvey(survey, false)).toList();
        return objectMapper.writeValueAsString(Map.of("total", result.size(), "surveys", result));
    }

    private Map<String, Object> toToolSurvey(SurveyDto survey, boolean includeQuestions) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", survey.getId());
        result.put("title", survey.getTitle());
        result.put("description", survey.getDescription());
        result.put("status", survey.getStatus());
        result.put("lifecycle", survey.getLifecycle());
        result.put("openingAt", survey.getOpeningAt());
        result.put("closingAt", survey.getClosingAt());
        result.put("featured", survey.getFeatured());
        result.put("submittedByMe", survey.getSubmittedByMe());
        result.put("submittedAtByMe", survey.getSubmittedAtByMe());
        result.put("acceptingResponses", survey.getAcceptingResponses());
        result.put("resultsVisible", survey.getResultsVisible());
        if (survey.getResponseCount() != null) {
            result.put("responseCount", survey.getResponseCount());
        }
        if (includeQuestions && survey.getQuestions() != null) {
            result.put("questions", survey.getQuestions().stream().map(this::toToolQuestion).toList());
        }
        result.put("route", "/modules/surveys/" + survey.getId());
        return result;
    }

    private Map<String, Object> toToolQuestion(SurveyQuestionDto question) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", question.getId());
        result.put("position", question.getPosition());
        result.put("prompt", question.getPrompt());
        result.put("type", question.getType());
        result.put("required", question.getRequired());
        result.put("options", question.getOptions());
        result.put("myValues", question.getMyValues());
        if (question.getResultCounts() != null) result.put("resultCounts", question.getResultCounts());
        if (question.getTextResults() != null) result.put("textResults", question.getTextResults());
        return result;
    }

    private boolean matches(SurveyDto survey, String query) {
        if (query == null || query.isBlank()) return true;
        String normalized = query.toLowerCase(Locale.ROOT);
        return stringValue(survey.getTitle()).toLowerCase(Locale.ROOT).contains(normalized)
                || stringValue(survey.getDescription()).toLowerCase(Locale.ROOT).contains(normalized);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return value == null ? null : Long.valueOf(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
