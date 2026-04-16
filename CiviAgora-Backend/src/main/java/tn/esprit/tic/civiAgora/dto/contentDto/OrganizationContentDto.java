package tn.esprit.tic.civiAgora.dto.contentDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationContentDto {
    private Long id;
    private String type;
    private String title;
    private String body;
    private List<String> options;
    private Boolean published;
    private String createdAt;
    private Integer organizationId;
    private Integer createdByUserId;
    private String createdByName;
    private String myAnswer;
    private Boolean myParticipating;
    private String myReaction;
}
