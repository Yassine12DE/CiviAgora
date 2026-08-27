package tn.esprit.tic.civiAgora.dto.surveyDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tn.esprit.tic.civiAgora.dao.entity.enums.SurveyResultVisibility;
import tn.esprit.tic.civiAgora.dao.entity.enums.SurveyStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SurveyUpsertRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 180, message = "Title must be 180 characters or fewer")
    private String title;
    private String description;
    private SurveyStatus status;
    private LocalDateTime openingAt;
    private LocalDateTime closingAt;
    private SurveyResultVisibility resultVisibility;
    private Boolean featured;
    @Valid
    private List<SurveyQuestionRequest> questions = new ArrayList<>();
}
