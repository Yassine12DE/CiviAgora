package tn.esprit.tic.civiAgora.controller.back_office;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dto.surveyDto.*;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.service.SurveyService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/org/{organizationId}/surveys")
@RequiredArgsConstructor
public class OrganizationSurveyController {
    private final SurveyService surveyService;
    private final RbacService rbacService;

    @GetMapping
    public List<SurveyDto> list(@PathVariable Integer organizationId) {
        rbacService.requireTenantContentAccess(organizationId);
        return surveyService.listForUser(organizationId, rbacService.getCurrentUserOrThrow());
    }

    @GetMapping("/{surveyId}")
    public SurveyDto get(@PathVariable Integer organizationId, @PathVariable Long surveyId) {
        rbacService.requireTenantContentAccess(organizationId);
        return surveyService.getForUser(organizationId, surveyId, rbacService.getCurrentUserOrThrow());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SurveyDto create(@PathVariable Integer organizationId, @Valid @RequestBody SurveyUpsertRequest request) {
        rbacService.requireTenantContentCreationAccess(organizationId);
        return surveyService.create(organizationId, request, rbacService.getCurrentUserOrThrow());
    }

    @PutMapping("/{surveyId}")
    public SurveyDto update(@PathVariable Integer organizationId, @PathVariable Long surveyId,
                            @Valid @RequestBody SurveyUpsertRequest request) {
        rbacService.requireTenantContentCreationAccess(organizationId);
        return surveyService.update(organizationId, surveyId, request, rbacService.getCurrentUserOrThrow());
    }

    @PostMapping("/{surveyId}/submissions")
    public SurveyDto submit(@PathVariable Integer organizationId, @PathVariable Long surveyId,
                            @Valid @RequestBody SurveySubmissionRequest request) {
        rbacService.requireTenantContentInteractionAccess(organizationId);
        User actor = rbacService.getCurrentUserOrThrow();
        return surveyService.submit(organizationId, surveyId, request, actor);
    }

    @GetMapping("/{surveyId}/results")
    public SurveyDto results(@PathVariable Integer organizationId, @PathVariable Long surveyId) {
        rbacService.requireTenantAnalyticsAccess(organizationId);
        return surveyService.getResults(organizationId, surveyId, rbacService.getCurrentUserOrThrow());
    }

    @GetMapping(value = "/{surveyId}/results.csv", produces = "text/csv")
    public ResponseEntity<byte[]> export(@PathVariable Integer organizationId, @PathVariable Long surveyId) {
        rbacService.requireTenantAnalyticsAccess(organizationId);
        byte[] body = surveyService.exportCsv(organizationId, surveyId).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=survey-" + surveyId + "-responses.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(body);
    }
}
