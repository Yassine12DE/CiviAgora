package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationContentType;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentDto;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentInteractionRequest;
import tn.esprit.tic.civiAgora.dto.surveyDto.SurveySubmissionRequest;
import tn.esprit.tic.civiAgora.service.ModuleRequestService;
import tn.esprit.tic.civiAgora.service.ModuleService;
import tn.esprit.tic.civiAgora.service.OrganizationContentService;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.service.SurveyService;
import tn.esprit.tic.civiAgora.service.UserService;
import tn.esprit.tic.civiAgora.tools.PendingCopilotActionService;
import tn.esprit.tic.civiAgora.tools.ToolDefinition;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrganizationActionTool implements ToolDefinition {
    private final ObjectMapper objectMapper;
    private final PendingCopilotActionService confirmations;
    private final RbacService rbacService;
    private final OrganizationContentService contentService;
    private final SurveyService surveyService;
    private final OrganizationModuleService moduleService;
    private final ModuleService catalogService;
    private final ModuleRequestService moduleRequestService;
    private final UserService userService;

    @Override
    public String getName() {
        return "organization_actions";
    }

    @Override
    public String getDescription() {
        return "Prepare or confirm supported persistent actions using existing CIVOX services. Every action is tenant/RBAC/module "
                + "checked and requires a separate explicit user confirmation. Never call confirm_pending unless the latest user message is confirmation.";
    }

    @Override
    public Operation getOperation() {
        return Operation.WRITE;
    }

    @Override
    public boolean requiresConfirmation() {
        return true;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.ofEntries(
                        Map.entry("action", Map.of("type", "string", "enum", List.of(
                                "prepare_content_publication", "prepare_module_visibility", "prepare_module_request",
                                "prepare_user_archive", "prepare_survey_submission", "prepare_content_response",
                                "confirm_pending", "cancel_pending"
                        ))),
                        Map.entry("contentType", Map.of("type", "string", "enum", List.of("VOTE", "CONCERTATION", "YOUTH_NEWS"))),
                        Map.entry("contentId", Map.of("type", "integer")),
                        Map.entry("published", Map.of("type", "boolean")),
                        Map.entry("answer", Map.of("type", "string")),
                        Map.entry("participating", Map.of("type", "boolean")),
                        Map.entry("reaction", Map.of("type", "string")),
                        Map.entry("surveyId", Map.of("type", "integer")),
                        Map.entry("answers", Map.of("type", "array", "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "questionId", Map.of("type", "integer"),
                                        "values", Map.of("type", "array", "items", Map.of("type", "string"))
                                ),
                                "required", List.of("questionId", "values")
                        ))),
                        Map.entry("moduleCode", Map.of("type", "string")),
                        Map.entry("enabled", Map.of("type", "boolean")),
                        Map.entry("comment", Map.of("type", "string")),
                        Map.entry("email", Map.of("type", "string")),
                        Map.entry("archived", Map.of("type", "boolean"))
                ),
                "required", List.of("action")
        );
    }

    @Override
    public String execute(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        String action = stringValue(input.get("action"));
        if ("confirm_pending".equals(action)) return confirm(context);
        if ("cancel_pending".equals(action)) {
            confirmations.clear(context);
            return json(Map.of("ok", true, "message", "The pending action was cancelled."));
        }
        if (confirmations.isExplicitConfirmation(context.currentUserMessage())) {
            return json(Map.of("ok", false, "confirmationRequired", true,
                    "message", "A confirmation message cannot prepare a different action. Confirm or cancel the existing pending action."));
        }

        return switch (action) {
            case "prepare_content_publication" -> prepareContentPublication(input, context);
            case "prepare_module_visibility" -> prepareModuleVisibility(input, context);
            case "prepare_module_request" -> prepareModuleRequest(input, context);
            case "prepare_user_archive" -> prepareUserArchive(input, context);
            case "prepare_survey_submission" -> prepareSurveySubmission(input, context);
            case "prepare_content_response" -> prepareContentResponse(input, context);
            default -> json(Map.of("ok", false, "message", "Unsupported action."));
        };
    }

    private String prepareContentPublication(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        rbacService.requireTenantContentCreationAccess(context.organizationId());
        OrganizationContentType type = contentType(input.get("contentType"));
        Long contentId = requiredLong(input, "contentId");
        Boolean published = requiredBoolean(input, "published");
        OrganizationContentDto target = contentService.getContent(context.organizationId(), type).stream()
                .filter(item -> contentId.equals(item.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Content not found in this organization"));
        return prepare(context, "CONTENT_PUBLICATION", Map.of(
                "contentType", type.name(), "contentId", contentId, "published", published
        ), (published ? "Publish" : "Unpublish") + " \"" + target.getTitle() + "\".");
    }

    private String prepareModuleVisibility(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        rbacService.requireTenantModuleVisibilityAccess(context.organizationId());
        String code = requiredText(input, "moduleCode").toUpperCase();
        Boolean enabled = requiredBoolean(input, "enabled");
        var target = moduleService.getAllModulesForOrganization(context.organizationId()).stream()
                .filter(module -> code.equalsIgnoreCase(module.getModuleCode())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Granted module not found in this organization"));
        return prepare(context, "MODULE_VISIBILITY", Map.of("moduleCode", code, "enabled", enabled),
                (enabled ? "Show" : "Hide") + " the " + target.getModuleName() + " module.");
    }

    private String prepareModuleRequest(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        rbacService.requireTenantModuleRequestAccess(context.organizationId());
        String code = requiredText(input, "moduleCode").toUpperCase();
        var catalogModule = catalogService.getTenantRequestableModules().stream()
                .filter(module -> code.equalsIgnoreCase(module.getCode())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("That module cannot be requested"));
        String comment = stringValue(input.get("comment"));
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("moduleCode", code);
        parameters.put("comment", comment);
        return prepare(context, "MODULE_REQUEST", parameters,
                "Request the " + catalogModule.getName() + " module for this organization.");
    }

    private String prepareUserArchive(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        rbacService.requireTenantUserManagementAccess(context.organizationId());
        String email = requiredText(input, "email");
        Boolean archived = requiredBoolean(input, "archived");
        User target = userService.getTenantUsers(context.organizationId()).stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found in this organization"));
        if (context.user().getRole() == Role.MANAGER && target.getRole() != Role.CITIZEN) {
            throw new org.springframework.security.access.AccessDeniedException("Managers can manage Citizen accounts only");
        }
        return prepare(context, "USER_ARCHIVE", Map.of("userId", target.getId(), "archived", archived),
                (archived ? "Archive" : "Restore") + " " + target.getEmail() + ".");
    }

    private String prepareSurveySubmission(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        rbacService.requireTenantContentInteractionAccess(context.organizationId());
        Long surveyId = requiredLong(input, "surveyId");
        Object answers = input.get("answers");
        if (!(answers instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("Survey answers are required");
        }
        var survey = surveyService.getForUser(context.organizationId(), surveyId, context.user());
        if (!Boolean.TRUE.equals(survey.getAcceptingResponses())) {
            throw new IllegalStateException("This survey is not accepting responses");
        }
        return prepare(context, "SURVEY_SUBMISSION", Map.of("surveyId", surveyId, "answers", answers),
                (Boolean.TRUE.equals(survey.getSubmittedByMe()) ? "Update" : "Submit") + " answers for \"" + survey.getTitle() + "\".");
    }

    private String prepareContentResponse(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        rbacService.requireTenantContentInteractionAccess(context.organizationId());
        OrganizationContentType type = contentType(input.get("contentType"));
        Long contentId = requiredLong(input, "contentId");
        OrganizationContentDto target = contentService.getVisibleContentForCurrentUser(context.organizationId(), type, context.user())
                .stream().filter(item -> contentId.equals(item.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Content not found in this organization"));
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("contentType", type.name());
        parameters.put("contentId", contentId);
        if (input.containsKey("answer")) parameters.put("answer", input.get("answer"));
        if (input.containsKey("participating")) parameters.put("participating", input.get("participating"));
        if (input.containsKey("reaction")) parameters.put("reaction", input.get("reaction"));
        if (parameters.size() == 2) throw new IllegalArgumentException("A response value is required");
        return prepare(context, "CONTENT_RESPONSE", parameters, "Save your response to \"" + target.getTitle() + "\".");
    }

    private String confirm(ToolExecutionContext context) throws Exception {
        PendingCopilotActionService.PendingAction pending = confirmations.requireConfirmed(context);
        Map<String, Object> parameters = pending.parameters();
        Object result = switch (pending.type()) {
            case "CONTENT_PUBLICATION" -> {
                rbacService.requireTenantContentCreationAccess(context.organizationId());
                yield contentService.updateContentPublicationStatus(context.organizationId(),
                        contentType(parameters.get("contentType")), longValue(parameters.get("contentId")),
                        booleanValue(parameters.get("published")));
            }
            case "MODULE_VISIBILITY" -> {
                rbacService.requireTenantModuleVisibilityAccess(context.organizationId());
                yield moduleService.updateTenantModuleVisibilityForOrganization(context.organizationId(),
                        stringValue(parameters.get("moduleCode")), booleanValue(parameters.get("enabled")));
            }
            case "MODULE_REQUEST" -> {
                rbacService.requireTenantModuleRequestAccess(context.organizationId());
                yield moduleRequestService.createTenantRequest(context.organizationId(),
                        stringValue(parameters.get("moduleCode")), stringValue(parameters.get("comment")));
            }
            case "USER_ARCHIVE" -> {
                rbacService.requireTenantUserManagementAccess(context.organizationId());
                yield userService.setTenantUserArchived(context.organizationId(),
                        integerValue(parameters.get("userId")), booleanValue(parameters.get("archived")), context.user());
            }
            case "SURVEY_SUBMISSION" -> {
                rbacService.requireTenantContentInteractionAccess(context.organizationId());
                SurveySubmissionRequest request = new SurveySubmissionRequest();
                request.setAnswers(objectMapper.convertValue(
                        parameters.get("answers"),
                        new TypeReference<List<tn.esprit.tic.civiAgora.dto.surveyDto.SurveyAnswerRequest>>() {}
                ));
                yield surveyService.submit(context.organizationId(), longValue(parameters.get("surveyId")), request, context.user());
            }
            case "CONTENT_RESPONSE" -> {
                rbacService.requireTenantContentInteractionAccess(context.organizationId());
                OrganizationContentInteractionRequest request = new OrganizationContentInteractionRequest();
                request.setAnswer(parameters.containsKey("answer") ? stringValue(parameters.get("answer")) : null);
                request.setParticipating(parameters.containsKey("participating")
                        ? booleanValue(parameters.get("participating")) : null);
                request.setReaction(parameters.containsKey("reaction") ? stringValue(parameters.get("reaction")) : null);
                yield contentService.saveCurrentUserResponse(context.organizationId(), contentType(parameters.get("contentType")),
                        longValue(parameters.get("contentId")), request, context.user());
            }
            default -> throw new IllegalStateException("Unsupported pending action");
        };
        confirmations.clear(context);
        return json(Map.of("ok", true, "executed", pending.summary(), "result", result));
    }

    private String prepare(ToolExecutionContext context, String type, Map<String, Object> parameters, String summary) throws Exception {
        confirmations.prepare(context, type, parameters, summary);
        return json(Map.of(
                "ok", true,
                "confirmationRequired", true,
                "summary", summary,
                "confirmationInstruction", "Reply exactly 'Confirm' to execute, or ask to cancel."
        ));
    }

    private OrganizationContentType contentType(Object value) {
        return OrganizationContentType.valueOf(requiredTextValue(value).toUpperCase());
    }

    private Long requiredLong(Map<String, Object> input, String key) {
        Long value = longValue(input.get(key));
        if (value == null) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private Boolean requiredBoolean(Map<String, Object> input, String key) {
        if (!(input.get(key) instanceof Boolean value)) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private String requiredText(Map<String, Object> input, String key) {
        return requiredTextValue(input.get(key));
    }

    private String requiredTextValue(Object value) {
        String text = stringValue(value);
        if (text.isBlank()) throw new IllegalArgumentException("A required value is missing");
        return text.trim();
    }

    private String stringValue(Object value) { return value == null ? "" : String.valueOf(value); }
    private Long longValue(Object value) { return value instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(value)); }
    private Integer integerValue(Object value) { return value instanceof Number n ? n.intValue() : Integer.valueOf(String.valueOf(value)); }
    private Boolean booleanValue(Object value) { return value instanceof Boolean b ? b : Boolean.valueOf(String.valueOf(value)); }
    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }

}
