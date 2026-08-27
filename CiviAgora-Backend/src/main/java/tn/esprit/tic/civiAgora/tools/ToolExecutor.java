package tn.esprit.tic.civiAgora.tools;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.exception.TenantAccessDeniedException;
import tn.esprit.tic.civiAgora.exception.TenantResolutionException;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.service.TenantAccessService;

import java.util.*;

@Slf4j
@Component
public class ToolExecutor {

    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();
    private final RbacService rbacService;
    private final TenantAccessService tenantAccessService;
    private final ObjectMapper objectMapper;

    public ToolExecutor(List<ToolDefinition> toolDefinitions,
                        RbacService rbacService,
                        TenantAccessService tenantAccessService,
                        ObjectMapper objectMapper) {
        this.rbacService = rbacService;
        this.tenantAccessService = tenantAccessService;
        this.objectMapper = objectMapper;
        for (ToolDefinition tool : toolDefinitions) {
            tools.put(tool.getName(), tool);
            log.info("Registered chatbot tool: {}", tool.getName());
        }
    }

    public List<Map<String, Object>> getToolsForApi() {
        return tools.values().stream()
                .map(t -> Map.<String, Object>of(
                        "name", t.getName(),
                        "description", t.getDescription()
                                + " [" + t.getOperation().name().toLowerCase(Locale.ROOT) + "]",
                        "input_schema", t.getInputSchema()
                ))
                .toList();
    }

    public String execute(String toolName, Map<String, Object> input, String currentUserMessage) {
        ToolDefinition tool = tools.get(toolName);
        if (tool == null) {
            return safeError("UNKNOWN_TOOL", "That capability is not available.");
        }

        try {
            User user = rbacService.getCurrentUserOrThrow();
            Organization organization = tenantAccessService.getCurrentOrganizationEntityOrThrow();
            if (user.getRole() != Role.SUPER_ADMIN
                    && (user.getOrganization() == null
                    || !organization.getId().equals(user.getOrganization().getId()))) {
                throw new TenantAccessDeniedException("Authenticated user does not belong to the resolved tenant");
            }

            ToolExecutionContext context = new ToolExecutionContext(user, organization, currentUserMessage);
            log.info("Executing chatbot tool: name={}, operation={}, organizationId={}, userId={}",
                    toolName, tool.getOperation(), context.organizationId(), context.userId());
            return tool.execute(input == null ? Map.of() : input, context);
        } catch (AccessDeniedException | TenantAccessDeniedException e) {
            log.warn("Chatbot tool access denied: name={}", toolName);
            return safeError("FORBIDDEN", "You don't have permission to use this capability.");
        } catch (TenantResolutionException e) {
            log.warn("Chatbot tenant resolution failed: name={}", toolName);
            return safeError("TENANT_UNAVAILABLE", "Your organization context could not be verified.");
        } catch (PendingCopilotActionService.ConfirmationRequiredException e) {
            return safeConfirmationError(e.getMessage());
        } catch (Exception e) {
            log.error("Tool {} failed", toolName, e);
            return safeError("TOOL_FAILED", "Unable to retrieve that information right now.");
        }
    }

    private String safeError(String code, String message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "ok", false,
                    "error", code,
                    "message", message
            ));
        } catch (Exception ignored) {
            return "{\"ok\":false,\"error\":\"TOOL_FAILED\",\"message\":\"Unable to complete that request.\"}";
        }
    }

    private String safeConfirmationError(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "ok", false,
                    "error", "CONFIRMATION_REQUIRED",
                    "confirmationRequired", true,
                    "message", message
            ));
        } catch (Exception ignored) {
            return safeError("CONFIRMATION_REQUIRED", "Explicit confirmation is required.");
        }
    }
}
