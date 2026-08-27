package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.mappers.organizationMappers.UserToOrganizationMapper;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.service.UserService;
import tn.esprit.tic.civiAgora.tools.ToolDefinition;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrganizationUserTool implements ToolDefinition {
    private final ObjectMapper objectMapper;
    private final RbacService rbacService;
    private final UserService userService;
    private final UserToOrganizationMapper userMapper;

    @Override
    public String getName() {
        return "organization_users";
    }

    @Override
    public String getDescription() {
        return "Authorized organization user management queries: counts, role summaries, recent users, and limited search/details. "
                + "Citizens and other unauthorized roles cannot use this tool.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of("type", "string", "enum", List.of(
                                "get_summary", "search_users", "list_by_role", "list_new_this_month"
                        )),
                        "query", Map.of("type", "string", "description", "Name or exact email to search"),
                        "role", Map.of("type", "string", "enum", List.of("ADMIN", "MANAGER", "MODERATOR", "CITIZEN"))
                ),
                "required", List.of("action")
        );
    }

    @Override
    public String execute(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        rbacService.requireTenantUserManagementAccess(context.organizationId());
        List<User> users = userService.getTenantUsers(context.organizationId());
        String action = String.valueOf(input.get("action"));

        Object result = switch (action) {
            case "get_summary" -> summary(users);
            case "search_users" -> listResult(users.stream()
                    .filter(user -> matches(user, stringValue(input.get("query"))))
                    .limit(20).toList());
            case "list_by_role" -> listResult(users.stream()
                    .filter(user -> user.getRole() != null
                            && user.getRole().name().equalsIgnoreCase(stringValue(input.get("role"))))
                    .limit(50).toList());
            case "list_new_this_month" -> listResult(users.stream()
                    .filter(this::createdThisMonth)
                    .limit(50).toList());
            default -> Map.of("ok", false, "message", "Unsupported user query.");
        };
        return objectMapper.writeValueAsString(result);
    }

    private Map<String, Object> summary(List<User> users) {
        long active = users.stream().filter(user -> user.isEnabled() && !Boolean.TRUE.equals(user.getArchived())).count();
        Map<String, Long> byRole = users.stream()
                .filter(user -> user.getRole() != null)
                .collect(Collectors.groupingBy(user -> user.getRole().name(), Collectors.counting()));
        return Map.of(
                "totalUsers", users.size(),
                "activeUsers", active,
                "inactiveOrArchivedUsers", users.size() - active,
                "newUsersThisMonth", users.stream().filter(this::createdThisMonth).count(),
                "usersByRole", byRole
        );
    }

    private Map<String, Object> listResult(List<User> users) {
        return Map.of(
                "total", users.size(),
                "users", users.stream().map(user -> {
                    var dto = userMapper.toUserToOrganizationDto(user);
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("name", (stringValue(dto.getFirstName()) + " " + stringValue(dto.getLastName())).trim());
                    value.put("email", dto.getEmail());
                    value.put("phone", dto.getPhone());
                    value.put("role", dto.getRole());
                    value.put("status", dto.getStatus());
                    value.put("createdAt", dto.getCreatedAt());
                    return value;
                }).toList()
        );
    }

    private boolean matches(User user, String query) {
        if (query.isBlank()) return false;
        String needle = query.toLowerCase(Locale.ROOT);
        return stringValue(user.getFirstName()).toLowerCase(Locale.ROOT).contains(needle)
                || stringValue(user.getLastName()).toLowerCase(Locale.ROOT).contains(needle)
                || stringValue(user.getEmail()).toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean createdThisMonth(User user) {
        Timestamp created = user.getCreatedTimestamp();
        return created != null && YearMonth.from(created.toLocalDateTime()).equals(YearMonth.from(LocalDateTime.now()));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
