package tn.esprit.tic.civiAgora.dto.analyticsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDashboardDto {
    private Integer organizationId;
    private String organizationSlug;
    private Boolean analyticsEnabled;
    private String status;
    private String message;
    private List<KpiCardDto> kpis;
    private List<ChartSeriesDto> charts;
    private List<ModuleActivityDto> moduleActivity;
    private List<RecentActivityDto> recentActivities;
    private List<String> insights;
}
