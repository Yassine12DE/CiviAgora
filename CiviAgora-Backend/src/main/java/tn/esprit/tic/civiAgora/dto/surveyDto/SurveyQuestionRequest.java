package tn.esprit.tic.civiAgora.dto.surveyDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.esprit.tic.civiAgora.dao.entity.enums.SurveyQuestionType;

import java.util.List;

@Data
public class SurveyQuestionRequest {
    @NotBlank(message = "Question text is required")
    private String prompt;
    @NotNull(message = "Question type is required")
    private SurveyQuestionType type;
    private Boolean required;
    private List<String> options;
}
