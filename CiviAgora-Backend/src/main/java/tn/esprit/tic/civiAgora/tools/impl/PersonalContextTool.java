package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.service.ModuleAccessService;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.tools.ToolDefinition;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PersonalContextTool implements ToolDefinition {
    private final ObjectMapper objectMapper;
    private final RbacService rbacService;
    private final ModuleAccessService moduleAccessService;
    private final OrganizationModuleService organizationModuleService;

    @Override
    public String getName() {
        return "personal_context";
    }

    @Override
    public String getDescription() {
        return "Get the authenticated user's own profile, role, effective permissions, and accessible modules. "
                + "Never use this tool for another user.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("action", Map.of(
                        "type", "string",
                        "enum", List.of("get_my_profile", "get_my_permissions")
                )),
                "required", List.of("action")
        );
    }

    @Override
    public String execute(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        String action = String.valueOf(input.get("action"));
        return switch (action) {
            case "get_my_profile" -> objectMapper.writeValueAsString(profile(context));
            case "get_my_permissions" -> objectMapper.writeValueAsString(permissions(context));
            default -> objectMapper.writeValueAsString(Map.of(
                    "ok", false,
                    "message", "Unsupported personal context action."
            ));
        };
    }

    private Map<String, Object> profile(ToolExecutionContext context) {
        User user = context.user();
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("birthDate", user.getBirthDate());
        profile.put("role", user.getRole() == null ? null : user.getRole().name());
        profile.put("enabled", user.isEnabled());
        profile.put("archived", Boolean.TRUE.equals(user.getArchived()));
        profile.put("accountStatus", user.isEnabled() && !Boolean.TRUE.equals(user.getArchived()) ? "ACTIVE" : "INACTIVE");
        profile.put("organizationName", context.organization().getName());
        profile.put("organizationSlug", context.organizationSlug());
        return profile;
    }

    private Map<String, Object> permissions(ToolExecutionContext context) {
        Integer organizationId = context.organizationId();
        List<String> granted = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        List<Map<String, Object>> accessibleModules = moduleAccessService.getModulesForCurrentUser();
        var backOfficeModules = organizationModuleService.getTenantModules(organizationId);
        boolean hasParticipationAuthoringModule = accessibleModules.stream()
                .map(module -> String.valueOf(module.get("code")))
                .anyMatch(code -> List.of("VOTE", "CONFERENCE", "YOUTHSPACE", "SURVEYS").contains(code));
        boolean analyticsEnabled = backOfficeModules.stream()
                .anyMatch(module -> "ANALYTICS".equalsIgnoreCase(module.getModuleCode())
                        && Boolean.TRUE.equals(module.getEnabledByOrganization()));

        permission("open the organization back office", () -> rbacService.requireTenantBackOfficeAccess(organizationId), granted, unavailable);
        permission("manage organization users", () -> rbacService.requireTenantUserManagementAccess(organizationId), granted, unavailable);
        permission("create and manage participation content", hasParticipationAuthoringModule,
                () -> rbacService.requireTenantContentCreationAccess(organizationId), granted, unavailable);
        permission("view organization analytics", analyticsEnabled,
                () -> rbacService.requireTenantAnalyticsAccess(organizationId), granted, unavailable);
        permission("change module visibility", () -> rbacService.requireTenantModuleVisibilityAccess(organizationId), granted, unavailable);
        permission("customize organization branding and settings", () -> rbacService.requireTenantDesignCustomizationAccess(organizationId), granted, unavailable);
        permission("request organization modules", () -> rbacService.requireTenantModuleRequestAccess(organizationId), granted, unavailable);

        return Map.of(
                "role", context.user().getRole().name(),
                "granted", granted,
                "unavailable", unavailable,
                "accessibleModules", accessibleModules
        );
    }

    private void permission(String label, Runnable check, List<String> granted, List<String> unavailable) {
        try {
            check.run();
            granted.add(label);
        } catch (RuntimeException denied) {
            unavailable.add(label);
        }
    }

    private void permission(String label, boolean prerequisite, Runnable check,
                            List<String> granted, List<String> unavailable) {
        if (!prerequisite) {
            unavailable.add(label);
            return;
        }
        permission(label, check, granted, unavailable);
    }
}
