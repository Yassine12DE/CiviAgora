package tn.esprit.tic.civiAgora.dto.analyticsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartSeriesDto {
    private String key;
    private String title;
    private String subtitle;
    private String chartType;
    private String xAxisLabel;
    private String yAxisLabel;
    private List<Map<String, Object>> points;
}
