package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.tools.ToolDefinition;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NavigationHelpTool implements ToolDefinition {
    private final ObjectMapper objectMapper;
    private final RbacService rbacService;
    private final OrganizationModuleService organizationModuleService;

    @Override
    public String getName() {
        return "navigation_help";
    }

    @Override
    public String getDescription() {
        return "Return validated existing CIVOX internal routes for the current role and enabled modules. "
                + "Never invent or accept a URL.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("destination", Map.of(
                        "type", "string",
                        "enum", List.of("home", "modules", "my_profile", "surveys", "manage_surveys", "survey_results",
                                "manage_users", "organization_overview", "organization_settings", "manage_modules", "module_requests")
                )),
                "required", List.of("destination")
        );
    }

    @Override
    public String execute(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        String destination = String.valueOf(input.get("destination"));
        Map<String, String> route = switch (destination) {
            case "home" -> link("Organization home", "/");
            case "modules" -> link("Browse modules", "/modules");
            case "my_profile" -> link("My profile", "/me");
            case "surveys" -> moduleEnabled(context, "SURVEYS") ? link("Open Surveys", "/modules/surveys") : null;
            case "manage_surveys" -> allowed(() -> rbacService.requireTenantContentCreationAccess(context.organizationId()))
                    && moduleEnabled(context, "SURVEYS") ? link("Manage Surveys", "/backoffice/surveys") : null;
            case "survey_results" -> allowed(() -> rbacService.requireTenantAnalyticsAccess(context.organizationId()))
                    && moduleEnabled(context, "SURVEYS") ? link("Survey results", "/backoffice/surveys") : null;
            case "manage_users" -> allowed(() -> rbacService.requireTenantUserManagementAccess(context.organizationId()))
                    ? link("Manage users", "/backoffice/users") : null;
            case "organization_overview" -> allowed(() -> rbacService.requireTenantBackOfficeAccess(context.organizationId()))
                    ? link("Organization overview", "/backoffice") : null;
            case "organization_settings" -> allowed(() -> rbacService.requireTenantDesignCustomizationAccess(context.organizationId()))
                    ? link("Organization settings", "/backoffice/design") : null;
            case "manage_modules" -> allowed(() -> rbacService.requireTenantModuleVisibilityAccess(context.organizationId()))
                    ? link("Manage modules", "/backoffice/modules") : null;
            case "module_requests" -> allowed(() -> rbacService.requireTenantModuleRequestAccess(context.organizationId()))
                    ? link("Module requests", "/backoffice/module-requests") : null;
            default -> null;
        };
        Object result = route == null
                ? Map.of("available", false, "message", "That destination is not available for your current role or enabled modules.")
                : Map.of("available", true, "link", route);
        return objectMapper.writeValueAsString(result);
    }

    private boolean moduleEnabled(ToolExecutionContext context, String code) {
        return organizationModuleService.getVisibleModulesForOrganization(context.organizationId()).stream()
                .anyMatch(module -> code.equalsIgnoreCase(module.getModuleCode()));
    }

    private boolean allowed(Runnable check) {
        try { check.run(); return true; }
        catch (RuntimeException denied) { return false; }
    }

    private Map<String, String> link(String label, String route) {
        return Map.of("label", label, "route", route);
    }
}
