package tn.esprit.tic.civiAgora.dto.analyticsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {
    private String type;
    private String title;
    private String description;
    private String createdAt;
    private String tone;
}
