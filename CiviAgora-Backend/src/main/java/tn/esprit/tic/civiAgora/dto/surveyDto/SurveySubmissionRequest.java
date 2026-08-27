package tn.esprit.tic.civiAgora.dto.surveyDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SurveySubmissionRequest {
    @NotEmpty(message = "At least one answer is required")
    @Valid
    private List<SurveyAnswerRequest> answers;
}
