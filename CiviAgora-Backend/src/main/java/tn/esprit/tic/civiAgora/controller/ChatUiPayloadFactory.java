package tn.esprit.tic.civiAgora.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dto.chatDto.ChatUiPayload;
import tn.esprit.tic.civiAgora.dto.chatDto.OllamaChatResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ChatUiPayloadFactory {
    private static final List<String> TOOL_PRIORITY = List.of(
            "organization_analytics", "surveys", "organization_users",
            "participation_discovery", "organization_modules"
    );
    private static final List<String> KPI_PRIORITY = List.of(
            "total_users", "interactions", "consultations", "votes", "surveys", "active_users",
            "survey_responses", "new_users_month", "pending_moderation", "requests", "recent_activity", "news",
            "participation_rate", "engagement_rate"
    );
    private static final Set<String> OMITTED_KPIS = Set.of("events");

    private final ObjectMapper objectMapper;

    public ChatUiPayload from(OllamaChatResult result, String organizationName) {
        if (result == null || result.toolResults() == null) return null;
        for (String toolName : TOOL_PRIORITY) {
            for (int i = result.toolResults().size() - 1; i >= 0; i--) {
                OllamaChatResult.ExecutedToolResult toolResult = result.toolResults().get(i);
                if (!toolName.equals(toolResult.toolName())) continue;
                JsonNode root = parseSuccessful(toolResult.resultJson());
                if (root == null) continue;
                ChatUiPayload payload = switch (toolName) {
                    case "organization_analytics" -> analytics(root, organizationName);
                    case "surveys" -> surveys(root, toolResult.arguments());
                    case "organization_users" -> users(root, toolResult.arguments());
                    case "participation_discovery" -> opportunities(root);
                    case "organization_modules" -> modules(root, toolResult.arguments());
                    default -> null;
                };
                if (payload != null) return payload;
            }
        }
        return null;
    }

    private ChatUiPayload analytics(JsonNode root, String organizationName) {
        if (!root.has("kpis") && !root.has("moduleActivity") && !root.has("recentActivities")) return null;

        Map<String, JsonNode> byKey = new LinkedHashMap<>();
        root.path("kpis").forEach(node -> byKey.put(text(node, "key"), node));
        List<ChatUiPayload.Kpi> allKpis = new ArrayList<>();
        for (String key : KPI_PRIORITY) {
            JsonNode node = byKey.get(key);
            if (node != null && !OMITTED_KPIS.contains(key)) allKpis.add(toKpi(node));
        }
        byKey.forEach((key, node) -> {
            if (!KPI_PRIORITY.contains(key) && !OMITTED_KPIS.contains(key)) allKpis.add(toKpi(node));
        });

        List<ChatUiPayload.Alert> alerts = new ArrayList<>();
        for (JsonNode node : root.path("kpis")) {
            String key = text(node, "key");
            double value = node.path("value").asDouble();
            if (isRate(key, text(node, "label")) && value > 100) {
                alerts.add(new ChatUiPayload.Alert(
                        "warning",
                        humanLabel(key) + " exceeds 100%",
                        display(node) + " is based on recorded interactions relative to the current active-member denominator, so it can exceed 100%."
                ));
            }
            if ("pending_moderation".equals(key) && value > 0) {
                long pending = (long) value;
                alerts.add(new ChatUiPayload.Alert(
                        "warning", countLabel(pending, "module request") + (pending == 1 ? " needs" : " need") + " attention",
                        "Review the pending requests in the organization back office."
                ));
            }
        }

        List<JsonNode> activeNodes = new ArrayList<>();
        int inactiveCount = 0;
        long maxInteractions = 0;
        for (JsonNode node : root.path("moduleActivity")) {
            long content = node.path("contentCount").asLong();
            long interactions = node.path("interactionCount").asLong();
            if (content == 0 && interactions == 0) {
                inactiveCount++;
            } else {
                activeNodes.add(node);
                maxInteractions = Math.max(maxInteractions, interactions);
            }
        }
        activeNodes.sort(Comparator
                .comparingLong((JsonNode node) -> node.path("interactionCount").asLong()).reversed()
                .thenComparing(node -> text(node, "moduleName")));
        final long maxActivity = maxInteractions;
        List<ChatUiPayload.ModuleActivity> moduleActivity = activeNodes.stream().map(node -> {
            double rate = node.path("participationRate").asDouble();
            long interactions = node.path("interactionCount").asLong();
            return new ChatUiPayload.ModuleActivity(
                    text(node, "moduleCode"), text(node, "moduleName"), node.path("contentCount").asLong(), interactions,
                    maxActivity == 0 ? 0 : round(interactions * 100.0 / maxActivity),
                    rate > 100 ? "Interactions per active member" : "Participation metric",
                    formatNumber(rate) + "%", rate > 100
            );
        }).toList();

        List<String> insights = strings(root.path("insights"), 3);
        List<ChatUiPayload.Item> recentItems = new ArrayList<>();
        root.path("recentActivities").forEach(node -> recentItems.add(new ChatUiPayload.Item(
                text(node, "createdAt") + ":" + text(node, "title"), iconForActivity(text(node, "type")),
                text(node, "title"), normalized(text(node, "type")), text(node, "description"), List.of(),
                node.hasNonNull("createdAt")
                        ? List.of(new ChatUiPayload.Meta("Recorded", text(node, "createdAt"), "clock")) : List.of(),
                null, null
        )));
        boolean recentOnly = !root.has("kpis") && !root.has("moduleActivity");
        return payload(
                "analytics",
                new ChatUiPayload.Header(recentOnly ? "Recent organization activity" : "Organization activity",
                        blankToNull(organizationName), "Updated just now"),
                allKpis.stream().limit(6).toList(), allKpis.stream().skip(6).toList(), insights,
                moduleActivity, inactiveCount, recentItems, alerts,
                List.of(new ChatUiPayload.Action("View analytics", "/backoffice", "barChart")), List.of()
        );
    }

    private ChatUiPayload surveys(JsonNode root, Map<String, Object> arguments) {
        String action = string(arguments == null ? null : arguments.get("action"));
        if (root.has("surveys")) {
            List<ChatUiPayload.Item> items = new ArrayList<>();
            root.path("surveys").forEach(node -> items.add(surveyItem(node)));
            String title = switch (action) {
                case "list_unanswered" -> "Surveys awaiting your response";
                case "list_answered" -> "Answered surveys";
                case "search" -> "Survey search results";
                default -> "Open surveys";
            };
            return payload("surveys", new ChatUiPayload.Header(title, countLabel(items.size(), "survey"), null),
                    List.of(), List.of(), List.of(), List.of(), 0, items, List.of(),
                    List.of(new ChatUiPayload.Action("Browse surveys", "/modules/surveys", "file")), List.of());
        }

        if (!root.has("title")) return null;
        List<ChatUiPayload.Chart> charts = new ArrayList<>();
        int writtenResponses = 0;
        for (JsonNode question : root.path("questions")) {
            JsonNode counts = question.path("resultCounts");
            if (counts.isObject() && !counts.isEmpty()) {
                long total = 0;
                Iterator<JsonNode> values = counts.elements();
                while (values.hasNext()) total += values.next().asLong();
                List<ChatUiPayload.ChartPoint> points = new ArrayList<>();
                Iterator<Map.Entry<String, JsonNode>> fields = counts.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    long value = entry.getValue().asLong();
                    points.add(new ChatUiPayload.ChartPoint(entry.getKey(), value,
                            total == 0 ? 0 : round(value * 100.0 / total)));
                }
                points.sort(Comparator.comparingLong(ChatUiPayload.ChartPoint::value).reversed());
                charts.add(new ChatUiPayload.Chart(text(question, "prompt"), countLabel(total, "response"), points));
            }
            if (question.path("textResults").isArray()) writtenResponses += question.path("textResults").size();
        }
        List<String> insights = writtenResponses > 0
                ? List.of(countLabel(writtenResponses, "authorized written response") + " available in the detailed results.")
                : List.of();
        List<ChatUiPayload.Kpi> kpis = root.hasNonNull("responseCount")
                ? List.of(new ChatUiPayload.Kpi("responses", "Responses", root.path("responseCount").asText(),
                "message", "primary", null, null)) : List.of();
        String id = root.path("id").asText();
        String route = "get_results".equals(action) && !id.isBlank() ? "/backoffice/surveys/" + id + "/results" : safeRoute(text(root, "route"));
        return payload("survey-results", new ChatUiPayload.Header(text(root, "title"), "Survey results", "Updated just now"),
                kpis, List.of(), insights, List.of(), 0, List.of(), List.of(),
                route == null ? List.of() : List.of(new ChatUiPayload.Action("View results", route, "barChart")), charts);
    }

    private ChatUiPayload users(JsonNode root, Map<String, Object> arguments) {
        if (root.has("totalUsers")) {
            List<ChatUiPayload.Kpi> kpis = List.of(
                    simpleKpi("totalUsers", "Total users", root, "users", "neutral"),
                    simpleKpi("activeUsers", "Active users", root, "checkCircle", "success"),
                    simpleKpi("newUsersThisMonth", "New this month", root, "userPlus", "primary"),
                    simpleKpi("inactiveOrArchivedUsers", "Inactive or archived", root, "users", "neutral")
            );
            return payload("user-summary", new ChatUiPayload.Header("Organization users", null, "Updated just now"),
                    kpis, List.of(), List.of(), List.of(), 0, List.of(), List.of(),
                    List.of(new ChatUiPayload.Action("Manage users", "/backoffice/users", "users")), List.of());
        }
        if (!root.has("users")) return null;
        List<ChatUiPayload.Item> items = new ArrayList<>();
        root.path("users").forEach(node -> items.add(new ChatUiPayload.Item(
                text(node, "email"), "users", text(node, "name"), text(node, "email"), null,
                List.of(status(text(node, "role")), status(text(node, "status"))),
                node.hasNonNull("phone") ? List.of(new ChatUiPayload.Meta("Phone", text(node, "phone"), "info")) : List.of(),
                null, null
        )));
        return payload("users", new ChatUiPayload.Header("User results", countLabel(items.size(), "user"), null),
                List.of(), List.of(), List.of(), List.of(), 0, items, List.of(),
                List.of(new ChatUiPayload.Action("Manage users", "/backoffice/users", "users")), List.of());
    }

    private ChatUiPayload opportunities(JsonNode root) {
        if (!root.has("items")) return null;
        List<ChatUiPayload.Item> items = new ArrayList<>();
        root.path("items").forEach(node -> {
            boolean participated = node.path("participatedByMe").asBoolean(false);
            boolean open = node.path("acceptingResponses").asBoolean(false);
            List<ChatUiPayload.Status> statuses = new ArrayList<>();
            statuses.add(new ChatUiPayload.Status(open ? "Open" : normalized(text(node, "lifecycle")), open ? "success" : "neutral"));
            statuses.add(new ChatUiPayload.Status(participated ? participatedLabel(text(node, "type")) : "Not answered", participated ? "primary" : "neutral"));
            List<ChatUiPayload.Meta> meta = node.hasNonNull("closingAt")
                    ? List.of(new ChatUiPayload.Meta("Closes", text(node, "closingAt"), "clock")) : List.of();
            items.add(new ChatUiPayload.Item(node.path("id").asText(), iconForType(text(node, "type")),
                    text(node, "title"), typeLabel(text(node, "type")), text(node, "summary"), statuses, meta,
                    safeRoute(text(node, "route")), open && !participated ? "Open" : "View"));
        });
        return payload("opportunities", new ChatUiPayload.Header("Participation opportunities", countLabel(items.size(), "result"), null),
                List.of(), List.of(), List.of(), List.of(), 0, items, List.of(), List.of(), List.of());
    }

    private ChatUiPayload modules(JsonNode root, Map<String, Object> arguments) {
        String action = string(arguments == null ? null : arguments.get("action"));
        String collection = root.has("requests") ? "requests" : root.has("modules") ? "modules" : null;
        if (collection == null) return null;
        List<ChatUiPayload.Item> items = new ArrayList<>();
        root.path(collection).forEach(node -> {
            boolean request = "requests".equals(collection);
            String title = request ? text(node, "moduleName") : text(node, "name");
            String subtitle = request ? text(node, "moduleCode") : text(node, "code");
            List<ChatUiPayload.Status> statuses = new ArrayList<>();
            if (request) statuses.add(status(text(node, "status")));
            else {
                if (node.has("enabledByOrganization")) statuses.add(new ChatUiPayload.Status(
                        node.path("enabledByOrganization").asBoolean() ? "Enabled" : "Disabled",
                        node.path("enabledByOrganization").asBoolean() ? "success" : "neutral"));
                statuses.add(status(text(node, "implementationStatus")));
            }
            List<ChatUiPayload.Meta> meta = request && node.hasNonNull("requestDate")
                    ? List.of(new ChatUiPayload.Meta("Requested", text(node, "requestDate"), "calendar")) : List.of();
            items.add(new ChatUiPayload.Item(node.path(request ? "id" : "code").asText(), "layers", title, subtitle,
                    request ? text(node, "comment") : text(node, "description"), statuses, meta, null, null));
        });
        boolean requests = "requests".equals(collection);
        String route = requests ? "/backoffice/module-requests" : "list_enabled".equals(action) ? "/modules" : "/backoffice/modules";
        return payload(requests ? "module-requests" : "modules",
                new ChatUiPayload.Header(requests ? "Module requests" : "Organization modules", countLabel(items.size(), requests ? "request" : "module"), null),
                List.of(), List.of(), List.of(), List.of(), 0, items, List.of(),
                List.of(new ChatUiPayload.Action(requests ? "View module requests" : "Open modules", route, "layers")), List.of());
    }

    private ChatUiPayload.Item surveyItem(JsonNode node) {
        boolean open = node.path("acceptingResponses").asBoolean(false);
        boolean answered = node.path("submittedByMe").asBoolean(false);
        List<ChatUiPayload.Status> statuses = List.of(
                new ChatUiPayload.Status(open ? "Open" : normalized(text(node, "lifecycle")), open ? "success" : "neutral"),
                new ChatUiPayload.Status(answered ? "Answered" : "Not answered", answered ? "primary" : "neutral")
        );
        List<ChatUiPayload.Meta> meta = node.hasNonNull("closingAt")
                ? List.of(new ChatUiPayload.Meta("Closes", text(node, "closingAt"), "clock")) : List.of();
        return new ChatUiPayload.Item(node.path("id").asText(), "file", text(node, "title"), "Survey",
                text(node, "description"), statuses, meta, safeRoute(text(node, "route")), open && !answered ? "Open" : "View");
    }

    private ChatUiPayload.Kpi toKpi(JsonNode node) {
        String key = text(node, "key");
        boolean abnormal = isRate(key, text(node, "label")) && node.path("value").asDouble() > 100;
        return new ChatUiPayload.Kpi(key, humanLabel(key), display(node), iconForKpi(key),
                abnormal ? "warning" : text(node, "tone"), text(node, "trend"),
                abnormal ? "Interaction-based metric" : null);
    }

    private ChatUiPayload.Kpi simpleKpi(String key, String label, JsonNode root, String icon, String tone) {
        return new ChatUiPayload.Kpi(key, label, root.path(key).asText("0"), icon, tone, null, null);
    }

    private ChatUiPayload payload(String type, ChatUiPayload.Header header, List<ChatUiPayload.Kpi> kpis,
                                  List<ChatUiPayload.Kpi> secondaryKpis, List<String> insights,
                                  List<ChatUiPayload.ModuleActivity> moduleActivity, Integer inactiveModuleCount,
                                  List<ChatUiPayload.Item> items, List<ChatUiPayload.Alert> alerts,
                                  List<ChatUiPayload.Action> actions, List<ChatUiPayload.Chart> charts) {
        return new ChatUiPayload(type, header, kpis, secondaryKpis, insights, moduleActivity, inactiveModuleCount,
                items, alerts, actions, charts);
    }

    private JsonNode parseSuccessful(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return root != null && !(root.has("ok") && !root.path("ok").asBoolean(true)) ? root : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> strings(JsonNode array, int limit) {
        List<String> result = new ArrayList<>();
        if (array.isArray()) array.forEach(node -> {
            if (result.size() < limit && !node.asText().isBlank()) result.add(node.asText());
        });
        return result;
    }

    private ChatUiPayload.Status status(String value) {
        String label = normalized(value);
        String upper = value == null ? "" : value.toUpperCase(Locale.ROOT);
        String tone;
        if (upper.contains("REJECTED")) tone = "danger";
        else if (upper.contains("INACTIVE") || upper.contains("ARCHIVED") || upper.contains("DISABLED")) tone = "neutral";
        else if (upper.contains("PENDING") || upper.contains("INCOMPLETE") || upper.contains("PLACEHOLDER")) tone = "warning";
        else if (upper.equals("ACTIVE") || upper.contains("ENABLED") || upper.contains("OPERATIONAL") || upper.contains("APPROVED")) tone = "success";
        else tone = "neutral";
        return new ChatUiPayload.Status(label, tone);
    }

    private String display(JsonNode node) {
        return node.hasNonNull("valueDisplay") ? node.path("valueDisplay").asText() : formatNumber(node.path("value").asDouble());
    }

    private String humanLabel(String key) {
        return switch (key) {
            case "total_users" -> "Total users";
            case "active_users" -> "Active users";
            case "new_users_month" -> "New this month";
            case "consultations" -> "Consultations";
            case "votes" -> "Voting items";
            case "requests" -> "Module requests";
            case "news" -> "Youth news";
            case "participation_rate" -> "Participation metric";
            case "engagement_rate" -> "Engagement metric";
            case "interactions" -> "Responses";
            case "surveys" -> "Surveys";
            case "survey_responses" -> "Survey responses";
            case "pending_moderation" -> "Pending requests";
            case "recent_activity" -> "Recent activity";
            default -> normalized(key);
        };
    }

    private String iconForKpi(String key) {
        return switch (key) {
            case "total_users", "active_users", "new_users_month" -> "users";
            case "interactions", "survey_responses" -> "message";
            case "votes" -> "vote";
            case "consultations", "surveys" -> "file";
            case "requests", "pending_moderation" -> "alert";
            case "participation_rate", "engagement_rate", "recent_activity" -> "activity";
            default -> "barChart";
        };
    }

    private String iconForType(String type) {
        return switch (type == null ? "" : type.toUpperCase(Locale.ROOT)) {
            case "VOTE" -> "vote";
            case "CONCERTATION" -> "message";
            case "SURVEY" -> "file";
            default -> "layers";
        };
    }

    private String iconForActivity(String type) {
        String normalized = type == null ? "" : type.toUpperCase(Locale.ROOT);
        if (normalized.contains("USER")) return "users";
        if (normalized.contains("SURVEY")) return "file";
        if (normalized.contains("REQUEST")) return "alert";
        if (normalized.contains("VOTE")) return "vote";
        return "activity";
    }

    private String typeLabel(String type) {
        return switch (type == null ? "" : type.toUpperCase(Locale.ROOT)) {
            case "VOTE" -> "Vote";
            case "CONCERTATION" -> "Consultation";
            case "YOUTH_NEWS" -> "Youth News";
            case "SURVEY" -> "Survey";
            default -> normalized(type);
        };
    }

    private String participatedLabel(String type) {
        return "VOTE".equalsIgnoreCase(type) ? "Already voted" : "Participated";
    }

    private boolean isRate(String key, String label) {
        String value = (key + " " + label).toLowerCase(Locale.ROOT);
        return value.contains("rate") || value.contains("percent") || value.contains("participation") || value.contains("engagement");
    }

    private String safeRoute(String route) {
        return route != null && route.startsWith("/") && !route.startsWith("//") ? route : null;
    }

    private String countLabel(long count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    private String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.path(field).asText() : "";
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) return "Available";
        String text = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(round(value));
    }
}
