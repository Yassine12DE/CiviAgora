package tn.esprit.tic.civiAgora.dto.contentDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.LocalDateTime;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationContentResultVisibility;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationContentRequest {
    private String title;
    private String body;
    private List<String> options;
    private Boolean published;
    private LocalDateTime openingAt;
    private LocalDateTime closingAt;
    private OrganizationContentResultVisibility resultVisibility;
    private Boolean featured;
}
