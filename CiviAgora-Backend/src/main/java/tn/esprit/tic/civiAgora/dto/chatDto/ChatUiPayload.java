package tn.esprit.tic.civiAgora.dto.chatDto;

import java.util.List;

/**
 * Optional, trusted presentation data for rich Copilot messages. Values in this
 * contract are projected from authorized tool output, never parsed from LLM text.
 */
public record ChatUiPayload(
        String type,
        Header header,
        List<Kpi> kpis,
        List<Kpi> secondaryKpis,
        List<String> insights,
        List<ModuleActivity> moduleActivity,
        Integer inactiveModuleCount,
        List<Item> items,
        List<Alert> alerts,
        List<Action> actions,
        List<Chart> charts
) {
    public record Header(String title, String subtitle, String freshness) {}

    public record Kpi(
            String key,
            String label,
            String value,
            String icon,
            String tone,
            String trend,
            String supportingText
    ) {}

    public record ModuleActivity(
            String code,
            String name,
            long contentCount,
            long interactionCount,
            double activityShare,
            String metricLabel,
            String metricValue,
            boolean abnormalMetric
    ) {}

    public record Item(
            String id,
            String icon,
            String title,
            String subtitle,
            String description,
            List<Status> statuses,
            List<Meta> meta,
            String route,
            String actionLabel
    ) {}

    public record Status(String label, String tone) {}

    public record Meta(String label, String value, String icon) {}

    public record Alert(String tone, String title, String message) {}

    public record Action(String label, String route, String icon) {}

    public record Chart(String title, String subtitle, List<ChartPoint> points) {}

    public record ChartPoint(String label, long value, double percentage) {}
}
