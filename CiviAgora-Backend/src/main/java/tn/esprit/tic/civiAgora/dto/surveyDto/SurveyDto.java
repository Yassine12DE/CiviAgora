package tn.esprit.tic.civiAgora.dto.surveyDto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SurveyDto {
    private Long id;
    private Integer organizationId;
    private String title;
    private String description;
    private String status;
    private String lifecycle;
    private String openingAt;
    private String closingAt;
    private String resultVisibility;
    private Boolean featured;
    private String createdAt;
    private String updatedAt;
    private Integer createdByUserId;
    private String createdByName;
    private Long responseCount;
    private Boolean submittedByMe;
    private String submittedAtByMe;
    private Boolean acceptingResponses;
    private Boolean resultsVisible;
    private List<SurveyQuestionDto> questions;
}
