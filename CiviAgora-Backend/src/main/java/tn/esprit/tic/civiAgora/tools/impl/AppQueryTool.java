package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.tools.ToolDefinition;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.repository.UserRepository;
import tn.esprit.tic.civiAgora.service.OrganizationService;
// Import your other repos/services as needed:
// import tn.esprit.tic.civiAgora.dao.repository.SurveyRepository;
// import tn.esprit.tic.civiAgora.dao.repository.ContentRepository;

import java.util.List;
import java.util.Map;

/**
 * ONE tool for all READ operations.
 * The LLM picks an "action" and optionally passes a "filter".
 * 
 * To add a new query: 
 *   1. Add the action name to the enum in getInputSchema()
 *   2. Add the action description in getDescription()
 *   3. Add a case in the switch + a private method
 *   That's it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppQueryTool implements ToolDefinition {

    private final ObjectMapper objectMapper;
    private final OrganizationService organizationService;
    private final UserRepository userRepository;
    // Add your repos:
    // private final SurveyRepository surveyRepository;
    // private final ContentRepository contentRepository;
    // private final ReclamationRepository reclamationRepository;

    @Override
    public String getName() {
        return "query_app_data";
    }

    @Override
    public String getDescription() {
        return """
                Query application data for the current organization. Available actions:
                - list_users: List all users with name, email, role
                - search_users: Search users by name or email (use filter param)
                - get_user_details: Get details of a specific user (use filter=email)
                - list_surveys: List all surveys
                - list_content: List published content/articles
                - list_reclamations: List reclamations/complaints
                - get_stats: Get organization statistics
                Use 'filter' parameter for search queries.
                """;
        // Update this description as you add more actions!
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of(
                                "type", "string",
                                "description", "The query action to perform",
                                "enum", List.of(
                                        "list_users",
                                        "search_users",
                                        "get_user_details",
                                        "list_surveys",
                                        "list_content",
                                        "list_reclamations",
                                        "get_stats"
                                )
                        ),
                        "filter", Map.of(
                                "type", "string",
                                "description", "Optional search/filter term"
                        )
                ),
                "required", List.of("action")
        );
    }

    @Override
    public String execute(Map<String, Object> input, ToolExecutionContext context) {
        Integer organizationId = context.organizationId();
        String action = (String) input.get("action");
        String filter = (String) input.getOrDefault("filter", null);

        log.info("AppQueryTool: action={}, filter={}, orgId={}", action, filter, organizationId);

        try {
            return switch (action) {
                case "list_users"        -> listUsers(organizationId);
                case "search_users"      -> searchUsers(organizationId, filter);
                case "get_user_details"  -> getUserDetails(organizationId, filter);
                case "list_surveys"      -> listSurveys(organizationId, filter);
                case "list_content"      -> listContent(organizationId, filter);
                case "list_reclamations" -> listReclamations(organizationId, filter);
                case "get_stats"         -> getStats(organizationId);
                default -> "{\"error\": \"Unknown action: " + action + "\"}";
            };
        } catch (Exception e) {
            log.error("AppQueryTool failed: action={}", action, e);
            return "{\"error\": \"Query failed: " + e.getMessage() + "\"}";
        }
    }

    // ===================================================
    // USERS
    // ===================================================

    private String listUsers(Integer orgId) throws Exception {
        List<User> users = organizationService.getUsersByOrganizationId(orgId);

        List<Map<String, String>> result = users.stream()
                .map(u -> Map.of(
                        "name", u.getFirstName() + " " + u.getLastName(),
                        "email", u.getEmail(),
                        "role", u.getRole().name()
                ))
                .toList();

        return objectMapper.writeValueAsString(Map.of(
                "total", result.size(),
                "users", result
        ));
    }

    private String searchUsers(Integer orgId, String filter) throws Exception {
        if (filter == null || filter.isBlank()) {
            return listUsers(orgId);
        }

        List<User> users = organizationService.getUsersByOrganizationId(orgId);
        String search = filter.toLowerCase();

        List<Map<String, String>> result = users.stream()
                .filter(u -> u.getFirstName().toLowerCase().contains(search)
                        || u.getLastName().toLowerCase().contains(search)
                        || u.getEmail().toLowerCase().contains(search))
                .map(u -> Map.of(
                        "name", u.getFirstName() + " " + u.getLastName(),
                        "email", u.getEmail(),
                        "role", u.getRole().name()
                ))
                .toList();

        return objectMapper.writeValueAsString(Map.of(
                "query", filter,
                "total", result.size(),
                "users", result
        ));
    }

    private String getUserDetails(Integer orgId, String filter) throws Exception {
        if (filter == null) return "{\"error\": \"Provide user email in filter\"}";

        List<User> users = organizationService.getUsersByOrganizationId(orgId);
        String search = filter.toLowerCase();

        return users.stream()
                .filter(u -> u.getEmail().toLowerCase().equals(search)
                        || (u.getFirstName() + " " + u.getLastName()).toLowerCase().contains(search))
                .findFirst()
                .map(u -> {
                    try {
                        return objectMapper.writeValueAsString(Map.of(
                                "name", u.getFirstName() + " " + u.getLastName(),
                                "email", u.getEmail(),
                                "role", u.getRole().name(),
                                "enabled", u.isEnabled()
                        ));
                    } catch (Exception e) {
                        return "{\"error\": \"Serialization failed\"}";
                    }
                })
                .orElse("{\"error\": \"User not found\"}");
    }

    // ===================================================
    // SURVEYS — uncomment when you have SurveyRepository
    // ===================================================

    private String listSurveys(Integer orgId, String filter) throws Exception {
        // TODO: Uncomment and adapt when SurveyRepository is available
        //
        // List<Survey> surveys = surveyRepository.findByOrganizationId(orgId);
        //
        // if (filter != null && !filter.isBlank()) {
        //     String search = filter.toLowerCase();
        //     surveys = surveys.stream()
        //             .filter(s -> s.getTitle().toLowerCase().contains(search))
        //             .toList();
        // }
        //
        // List<Map<String, Object>> result = surveys.stream()
        //         .map(s -> Map.<String, Object>of(
        //                 "id", s.getId(),
        //                 "title", s.getTitle(),
        //                 "status", s.getStatus().name(),
        //                 "createdAt", s.getCreatedAt().toString()
        //         ))
        //         .toList();
        //
        // return objectMapper.writeValueAsString(Map.of(
        //         "total", result.size(),
        //         "surveys", result
        // ));

        return "{\"message\": \"Survey query not yet implemented\"}";
    }

    // ===================================================
    // CONTENT — uncomment when you have ContentRepository
    // ===================================================

    private String listContent(Integer orgId, String filter) throws Exception {
        // TODO: Same pattern as surveys
        //
        // List<Content> contents = contentRepository.findByOrganizationId(orgId);
        // ...

        return "{\"message\": \"Content query not yet implemented\"}";
    }

    // ===================================================
    // RECLAMATIONS — uncomment when ready
    // ===================================================

    private String listReclamations(Integer orgId, String filter) throws Exception {
        // TODO: Uncomment when ReclamationRepository exists
        //
        // List<Reclamation> recs = reclamationRepository.findByOrganizationId(orgId);
        // ...

        return "{\"message\": \"Reclamation query not yet implemented\"}";
    }

    // ===================================================
    // STATS — aggregate dashboard data
    // ===================================================

    private String getStats(Integer orgId) throws Exception {
        var org = organizationService.getOrganizationById(orgId);
        List<User> users = organizationService.getUsersByOrganizationId(orgId);

        long activeUsers = users.stream().filter(User::isEnabled).count();

        return objectMapper.writeValueAsString(Map.of(
                "organizationName", org.getName(),
                "totalUsers", users.size(),
                "activeUsers", activeUsers,
                "inactiveUsers", users.size() - activeUsers,
                "processesCount", org.getProcessesCount(),
                "subscriptionPlan", org.getSubscriptionPlanCode() != null
                        ? org.getSubscriptionPlanCode() : "N/A",
                "subscriptionStatus", org.getSubscriptionStatus() != null
                        ? org.getSubscriptionStatus().name() : "N/A"
        ));
    }
}
