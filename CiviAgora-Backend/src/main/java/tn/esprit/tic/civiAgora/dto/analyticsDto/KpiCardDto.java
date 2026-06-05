package tn.esprit.tic.civiAgora.dto.analyticsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiCardDto {
    private String key;
    private String label;
    private Double value;
    private String valueDisplay;
    private String tone;
    private String trend;
}
