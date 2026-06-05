package tn.esprit.tic.civiAgora.dto.analyticsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleActivityDto {
    private String moduleCode;
    private String moduleName;
    private Long contentCount;
    private Long interactionCount;
    private Double participationRate;
}
