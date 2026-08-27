package tn.esprit.tic.civiAgora.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tn.esprit.tic.civiAgora.dto.chatDto.ChatUiPayload;
import tn.esprit.tic.civiAgora.dto.chatDto.OllamaChatResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChatUiPayloadFactoryTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatUiPayloadFactory factory = new ChatUiPayloadFactory(objectMapper);

    @Test
    void analyticsPayloadPrioritizesKpisRanksActivityAndFlagsAbnormalRates() throws Exception {
        Map<String, Object> data = Map.of(
                "kpis", List.of(
                        kpi("events", "Total events", 0, "0"),
                        kpi("total_users", "Total users", 7, "7"),
                        kpi("active_users", "Active users", 1, "1"),
                        kpi("interactions", "Responses", 11, "11"),
                        kpi("consultations", "Consultations", 3, "3"),
                        kpi("votes", "Votes", 2, "2"),
                        kpi("surveys", "Surveys", 4, "4"),
                        kpi("participation_rate", "Participation rate", 400, "400%"),
                        kpi("recent_activity", "Recent", 8, "8")
                ),
                "moduleActivity", List.of(
                        module("VOTE", "Voting", 2, 3, 300),
                        module("CONFERENCE", "Concertation", 3, 4, 400),
                        module("EVENTS", "Events", 0, 0, 0)
                ),
                "insights", List.of("Concertation is the most active module.")
        );

        ChatUiPayload payload = factory.from(result("organization_analytics", "get_dashboard", data), "Municipality of Tunis");

        assertNotNull(payload);
        assertEquals("analytics", payload.type());
        assertEquals("Municipality of Tunis", payload.header().subtitle());
        assertEquals(6, payload.kpis().size());
        assertTrue(payload.secondaryKpis().stream().noneMatch(kpi -> "events".equals(kpi.key())));
        assertEquals("Concertation", payload.moduleActivity().get(0).name());
        assertEquals(1, payload.inactiveModuleCount());
        assertTrue(payload.moduleActivity().get(0).abnormalMetric());
        assertEquals(1, payload.alerts().size());
        assertTrue(payload.alerts().get(0).message().contains("active-member denominator"));
        assertEquals("/backoffice", payload.actions().get(0).route());
    }

    @Test
    void surveyResultsUseAuthorizedCountsForBarsAndRegisteredResultsRoute() throws Exception {
        Map<String, Object> data = Map.of(
                "id", 42,
                "title", "Mobility Survey",
                "responseCount", 10,
                "questions", List.of(Map.of(
                        "prompt", "Preferred transport",
                        "resultCounts", Map.of("Public transport", 6, "Cycling", 3, "Other", 1),
                        "textResults", List.of("More reliable buses")
                ))
        );

        ChatUiPayload payload = factory.from(result("surveys", "get_results", data), "Tunis");

        assertNotNull(payload);
        assertEquals("survey-results", payload.type());
        assertEquals("10", payload.kpis().get(0).value());
        assertEquals(60.0, payload.charts().get(0).points().get(0).percentage());
        assertEquals("/backoffice/surveys/42/results", payload.actions().get(0).route());
        assertEquals(1, payload.insights().size());
    }

    @Test
    void listPayloadsExposeOnlyToolFieldsAndKnownNavigation() throws Exception {
        Map<String, Object> users = Map.of("total", 1, "users", List.of(Map.of(
                "name", "Mohamed Ben Ali", "email", "mohamed@example.test", "role", "CITIZEN", "status", "ACTIVE"
        )));
        ChatUiPayload userPayload = factory.from(result("organization_users", "search_users", users), "Tunis");
        assertEquals("Mohamed Ben Ali", userPayload.items().get(0).title());
        assertEquals("mohamed@example.test", userPayload.items().get(0).subtitle());
        assertNull(userPayload.items().get(0).route());
        assertEquals("/backoffice/users", userPayload.actions().get(0).route());

        Map<String, Object> modules = Map.of("modules", List.of(Map.of(
                "code", "SURVEYS", "name", "Surveys", "description", "Collect responses",
                "enabledByOrganization", true, "implementationStatus", "OPERATIONAL"
        )));
        ChatUiPayload modulePayload = factory.from(result("organization_modules", "list_enabled", modules), "Tunis");
        assertEquals("modules", modulePayload.type());
        assertEquals("Enabled", modulePayload.items().get(0).statuses().get(0).label());
        assertEquals("/modules", modulePayload.actions().get(0).route());
    }

    @Test
    void failedToolOutputNeverCreatesStructuredUi() {
        OllamaChatResult result = new OllamaChatResult("Denied", List.of(
                new OllamaChatResult.ExecutedToolResult("organization_users", Map.of("action", "search_users"),
                        "{\"ok\":false,\"error\":\"FORBIDDEN\"}")
        ));

        assertNull(factory.from(result, "Tunis"));
    }

    private OllamaChatResult result(String tool, String action, Object data) throws Exception {
        return new OllamaChatResult("Short summary", List.of(
                new OllamaChatResult.ExecutedToolResult(tool, Map.of("action", action), objectMapper.writeValueAsString(data))
        ));
    }

    private Map<String, Object> kpi(String key, String label, double value, String display) {
        return Map.of("key", key, "label", label, "value", value, "valueDisplay", display, "tone", "neutral");
    }

    private Map<String, Object> module(String code, String name, long content, long interactions, double rate) {
        return Map.of("moduleCode", code, "moduleName", name, "contentCount", content,
                "interactionCount", interactions, "participationRate", rate);
    }
}
