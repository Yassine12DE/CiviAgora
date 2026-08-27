package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationContentType;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentDto;
import tn.esprit.tic.civiAgora.dto.surveyDto.SurveyDto;
import tn.esprit.tic.civiAgora.service.OrganizationContentService;
import tn.esprit.tic.civiAgora.service.SurveyService;
import tn.esprit.tic.civiAgora.tools.ToolDefinition;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ParticipationDiscoveryTool implements ToolDefinition {
    private final ObjectMapper objectMapper;
    private final OrganizationContentService contentService;
    private final SurveyService surveyService;

    @Override
    public String getName() {
        return "participation_discovery";
    }

    @Override
    public String getDescription() {
        return "Discover real open votes, consultations, Youth News, and surveys in the current tenant, search them, "
                + "or show the current user's own participation. Open unanswered items closing soon are ranked first.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of("type", "string", "enum", List.of(
                                "discover_open", "closing_soon", "search", "my_participation", "list_votes",
                                "list_consultations", "list_youth_news", "get_content_detail"
                        )),
                        "query", Map.of("type", "string", "description", "Optional title/body search text"),
                        "contentId", Map.of("type", "integer", "description", "Content id returned by this tool"),
                        "contentType", Map.of("type", "string", "enum", List.of("VOTE", "CONCERTATION", "YOUTH_NEWS"))
                ),
                "required", List.of("action")
        );
    }

    @Override
    public String execute(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        String action = String.valueOf(input.get("action"));
        String query = stringValue(input.get("query"));
        List<Map<String, Object>> items = collectContent(context);

        if ("get_content_detail".equals(action)) {
            Long contentId = longValue(input.get("contentId"));
            String type = stringValue(input.get("contentType"));
            return objectMapper.writeValueAsString(items.stream()
                    .filter(item -> contentId != null && contentId.equals(item.get("id")))
                    .filter(item -> type.isBlank() || type.equals(item.get("type")))
                    .findFirst()
                    .orElse(Map.of("ok", false, "message", "Content not found in your organization.")));
        }

        if ("discover_open".equals(action) || "closing_soon".equals(action) || "search".equals(action)
                || "my_participation".equals(action)) {
            items.addAll(collectSurveys(context));
        }

        List<Map<String, Object>> filtered = items.stream()
                .filter(item -> switch (action) {
                    case "discover_open" -> Boolean.TRUE.equals(item.get("acceptingResponses"));
                    case "closing_soon" -> isClosingSoon(item);
                    case "search" -> matches(item, query);
                    case "my_participation" -> Boolean.TRUE.equals(item.get("participatedByMe"));
                    case "list_votes" -> "VOTE".equals(item.get("type"));
                    case "list_consultations" -> "CONCERTATION".equals(item.get("type"));
                    case "list_youth_news" -> "YOUTH_NEWS".equals(item.get("type"));
                    default -> false;
                })
                .sorted(discoveryOrder())
                .limit(30)
                .toList();

        return objectMapper.writeValueAsString(Map.of("total", filtered.size(), "items", filtered));
    }

    private List<Map<String, Object>> collectContent(ToolExecutionContext context) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (OrganizationContentType type : OrganizationContentType.values()) {
            try {
                contentService.getVisibleContentForCurrentUser(context.organizationId(), type, context.user())
                        .forEach(item -> result.add(toToolContent(item)));
            } catch (AccessDeniedException ignored) {
                // Disabled/unavailable modules are omitted from cross-module discovery.
            }
        }
        return result;
    }

    private List<Map<String, Object>> collectSurveys(ToolExecutionContext context) {
        try {
            return surveyService.listForUser(context.organizationId(), context.user()).stream()
                    .map(this::toToolSurvey)
                    .toList();
        } catch (AccessDeniedException ignored) {
            return List.of();
        }
    }

    private Map<String, Object> toToolContent(OrganizationContentDto item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("type", item.getType());
        result.put("title", item.getTitle());
        result.put("summary", item.getBody());
        result.put("options", item.getOptions());
        result.put("lifecycle", item.getLifecycle());
        result.put("openingAt", item.getOpeningAt());
        result.put("closingAt", item.getClosingAt());
        result.put("createdAt", item.getCreatedAt());
        result.put("acceptingResponses", item.getAcceptingResponses());
        result.put("resultsVisible", item.getResultsVisible());
        boolean participated = item.getMyAnswer() != null || item.getMyParticipating() != null || item.getMyReaction() != null;
        result.put("participatedByMe", participated);
        result.put("myAnswer", item.getMyAnswer());
        result.put("myParticipating", item.getMyParticipating());
        result.put("myReaction", item.getMyReaction());
        result.put("participatedAt", item.getMyRespondedAt());
        if (Boolean.TRUE.equals(item.getResultsVisible())) {
            result.put("totalResponses", item.getTotalResponses());
            result.put("responseBreakdown", item.getResponseBreakdown());
        }
        result.put("route", routeFor(item.getType(), item.getId()));
        return result;
    }

    private Map<String, Object> toToolSurvey(SurveyDto survey) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", survey.getId());
        result.put("type", "SURVEY");
        result.put("title", survey.getTitle());
        result.put("summary", survey.getDescription());
        result.put("lifecycle", survey.getLifecycle());
        result.put("openingAt", survey.getOpeningAt());
        result.put("closingAt", survey.getClosingAt());
        result.put("createdAt", survey.getCreatedAt());
        result.put("acceptingResponses", survey.getAcceptingResponses());
        result.put("participatedByMe", survey.getSubmittedByMe());
        result.put("participatedAt", survey.getSubmittedAtByMe());
        result.put("route", "/modules/surveys/" + survey.getId());
        return result;
    }

    private Comparator<Map<String, Object>> discoveryOrder() {
        return Comparator
                .comparingInt((Map<String, Object> item) -> priority(item))
                .thenComparing(item -> dateValue(item.get("closingAt")), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> dateValue(item.get("createdAt")), Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int priority(Map<String, Object> item) {
        boolean open = Boolean.TRUE.equals(item.get("acceptingResponses"));
        boolean answered = Boolean.TRUE.equals(item.get("participatedByMe"));
        boolean closing = item.get("closingAt") != null;
        if (open && !answered && closing) return 0;
        if (open && !answered) return 1;
        if (open) return 2;
        return 3;
    }

    private boolean matches(Map<String, Object> item, String query) {
        if (query.isBlank()) return true;
        String needle = query.toLowerCase(Locale.ROOT);
        return stringValue(item.get("title")).toLowerCase(Locale.ROOT).contains(needle)
                || stringValue(item.get("summary")).toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean isClosingSoon(Map<String, Object> item) {
        if (!Boolean.TRUE.equals(item.get("acceptingResponses"))) return false;
        LocalDateTime closingAt = dateValue(item.get("closingAt"));
        LocalDateTime now = LocalDateTime.now();
        return closingAt != null && !closingAt.isBefore(now) && !closingAt.isAfter(now.plusDays(7));
    }

    private LocalDateTime dateValue(Object value) {
        try { return value == null ? null : LocalDateTime.parse(String.valueOf(value)); }
        catch (DateTimeParseException ignored) { return null; }
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return value == null ? null : Long.valueOf(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String routeFor(String type, Long id) {
        String slug = switch (type == null ? "" : type) {
            case "VOTE" -> "vote";
            case "CONCERTATION" -> "concertation";
            case "YOUTH_NEWS" -> "youth-news";
            default -> "modules";
        };
        return "/modules/" + slug + "/" + id;
    }
}
