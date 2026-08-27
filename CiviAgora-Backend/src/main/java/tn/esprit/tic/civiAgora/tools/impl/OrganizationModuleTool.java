package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.service.ModuleRequestService;
import tn.esprit.tic.civiAgora.service.ModuleService;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.tools.ToolDefinition;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrganizationModuleTool implements ToolDefinition {
    private final ObjectMapper objectMapper;
    private final OrganizationModuleService organizationModuleService;
    private final ModuleService moduleService;
    private final ModuleRequestService moduleRequestService;
    private final RbacService rbacService;

    @Override
    public String getName() {
        return "organization_modules";
    }

    @Override
    public String getDescription() {
        return "Inspect real enabled modules, authorized hidden/granted module state, requestable modules, pending requests, "
                + "or explain a module and its current implementation status.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of("type", "string", "enum", List.of(
                                "list_enabled", "list_all_states", "is_enabled", "explain_module",
                                "list_requestable", "list_module_requests"
                        )),
                        "moduleCode", Map.of("type", "string", "description", "Catalog module code or name")
                ),
                "required", List.of("action")
        );
    }

    @Override
    public String execute(Map<String, Object> input, ToolExecutionContext context) throws Exception {
        String action = String.valueOf(input.get("action"));
        Object result = switch (action) {
            case "list_enabled" -> Map.of("modules", organizationModuleService
                    .getVisibleModulesForOrganization(context.organizationId()).stream().map(this::moduleState).toList());
            case "list_all_states" -> {
                rbacService.requireTenantBackOfficeAccess(context.organizationId());
                yield Map.of("modules", organizationModuleService
                        .getAllModulesForOrganization(context.organizationId()).stream().map(this::moduleState).toList());
            }
            case "is_enabled", "explain_module" -> explain(context, stringValue(input.get("moduleCode")));
            case "list_requestable" -> {
                rbacService.requireTenantModuleRequestAccess(context.organizationId());
                yield Map.of("modules", moduleService.getTenantRequestableModules().stream().map(this::catalogModule).toList());
            }
            case "list_module_requests" -> {
                rbacService.requireTenantModuleRequestAccess(context.organizationId());
                yield Map.of("requests", moduleRequestService.getTenantRequestsByOrganization(context.organizationId()));
            }
            default -> Map.of("ok", false, "message", "Unsupported module query.");
        };
        return objectMapper.writeValueAsString(result);
    }

    private Map<String, Object> explain(ToolExecutionContext context, String reference) {
        Module catalog = moduleService.getAllModules().stream()
                .filter(module -> module.getCode().equalsIgnoreCase(reference) || module.getName().equalsIgnoreCase(reference))
                .findFirst().orElse(null);
        if (catalog == null) {
            return Map.of("found", false, "message", "That module is not in the CIVOX catalog.");
        }
        OrganizationModuleDto grant = organizationModuleService.getAllModulesForOrganization(context.organizationId()).stream()
                .filter(module -> catalog.getCode().equalsIgnoreCase(module.getModuleCode()))
                .findFirst().orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.putAll(catalogModule(catalog));
        result.put("grantedBySaas", grant != null && Boolean.TRUE.equals(grant.getGrantedBySaas()));
        result.put("enabledByOrganization", grant != null && Boolean.TRUE.equals(grant.getEnabledByOrganization()));
        result.put("implementationStatus", implementationStatus(catalog.getCode()));
        return result;
    }

    private Map<String, Object> moduleState(OrganizationModuleDto module) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", module.getModuleCode());
        result.put("name", module.getModuleName());
        result.put("description", module.getModuleDescription());
        result.put("scope", module.getModuleScope());
        result.put("grantedBySaas", module.getGrantedBySaas());
        result.put("enabledByOrganization", module.getEnabledByOrganization());
        result.put("implementationStatus", implementationStatus(module.getModuleCode()));
        return result;
    }

    private Map<String, Object> catalogModule(Module module) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", module.getCode());
        result.put("name", module.getName());
        result.put("description", module.getDescription());
        result.put("scope", module.getScope() == null ? null : module.getScope().name());
        result.put("active", module.getActive());
        result.put("implementationStatus", implementationStatus(module.getCode()));
        return result;
    }

    private String implementationStatus(String code) {
        return switch (code == null ? "" : code.toUpperCase(Locale.ROOT)) {
            case "VOTE", "CONFERENCE", "YOUTHSPACE", "SURVEYS", "ANALYTICS" -> "OPERATIONAL";
            case "EVENTS", "COMPLAINTS" -> "PLACEHOLDER";
            case "NEWS" -> "INCOMPLETE";
            default -> "CATALOG_ONLY_OR_UNKNOWN";
        };
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
