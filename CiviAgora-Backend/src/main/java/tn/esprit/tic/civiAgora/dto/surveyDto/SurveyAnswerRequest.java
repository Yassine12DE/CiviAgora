package tn.esprit.tic.civiAgora.dto.surveyDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SurveyAnswerRequest {
    @NotNull(message = "Question id is required")
    private Long questionId;
    private List<String> values;
}
