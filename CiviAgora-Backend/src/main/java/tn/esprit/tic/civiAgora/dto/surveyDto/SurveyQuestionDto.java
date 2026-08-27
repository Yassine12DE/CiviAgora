package tn.esprit.tic.civiAgora.dto.surveyDto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class SurveyQuestionDto {
    private Long id;
    private Integer position;
    private String prompt;
    private String type;
    private Boolean required;
    private List<String> options;
    private List<String> myValues;
    private Map<String, Long> resultCounts;
    private List<String> textResults;
}
