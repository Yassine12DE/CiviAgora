package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dto.analyticsDto.AnalyticsDashboardDto;
import tn.esprit.tic.civiAgora.service.OrganizationAnalyticsService;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.tools.ToolDefinition;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrganizationAnalyticsTool implements ToolDefinition {
    private final ObjectMapper objectMapper;
    private final RbacService rbacService;
    private final OrganizationAnalyticsService analyticsService;

    @Override
    public String getName() {
        return "organization_analytics";
    }

    @Override
    public String getDescription() {
        return "Retrieve the authorized current tenant analytics dashboard, KPIs, module activity, trends, recent activity, "
                + "and trusted insights. Use it for organization summaries and briefings.";
    }

    @Override
    public String getRequiredModule() {
        return "ANALYTICS";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("action", Map.of(
                        "type", "string",
                        "enum", List.of("get_dashboard", "get_organization_summary", "get_recent_activity")
                )),
                "required", List.of("action")
        );
    }

    @Override
    public String execute(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        rbacService.requireTenantAnalyticsAccess(context.organizationId());
        AnalyticsDashboardDto dashboard = analyticsService.getDashboard(context.organizationId());
        String action = String.valueOf(input.get("action"));
        Object result = switch (action) {
            case "get_dashboard" -> Map.of(
                    "kpis", dashboard.getKpis(),
                    "charts", dashboard.getCharts(),
                    "moduleActivity", dashboard.getModuleActivity(),
                    "recentActivities", dashboard.getRecentActivities(),
                    "insights", dashboard.getInsights()
            );
            case "get_organization_summary" -> Map.of(
                    "organizationName", context.organization().getName(),
                    "kpis", dashboard.getKpis(),
                    "moduleActivity", dashboard.getModuleActivity(),
                    "insights", dashboard.getInsights()
            );
            case "get_recent_activity" -> Map.of("recentActivities", dashboard.getRecentActivities());
            default -> Map.of("ok", false, "message", "Unsupported analytics action.");
        };
        return objectMapper.writeValueAsString(result);
    }
}
