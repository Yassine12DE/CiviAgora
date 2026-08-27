package tn.esprit.tic.civiAgora.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.service.TenantAccessService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ToolExecutorTest {

    @Test
    void resolvesIdentityAndTenantServerSideAndIgnoresModelIds() {
        RbacService rbac = mock(RbacService.class);
        TenantAccessService tenantAccess = mock(TenantAccessService.class);
        Organization organization = organization(7, "tenant-a");
        User user = User.builder().id(11).role(Role.CITIZEN).organization(organization).build();
        when(rbac.getCurrentUserOrThrow()).thenReturn(user);
        when(tenantAccess.getCurrentOrganizationEntityOrThrow()).thenReturn(organization);

        AtomicReference<ToolExecutionContext> received = new AtomicReference<>();
        ToolDefinition tool = capturingTool(received);
        ToolExecutor executor = new ToolExecutor(List.of(tool), rbac, tenantAccess, new ObjectMapper());

        String result = executor.execute("test_tool", Map.of("organizationId", 999, "userId", 888), "question");

        assertEquals("{\"ok\":true}", result);
        assertEquals(7, received.get().organizationId());
        assertEquals(11, received.get().userId());
    }

    @Test
    void rejectsCrossTenantPrincipalBeforeToolRuns() {
        RbacService rbac = mock(RbacService.class);
        TenantAccessService tenantAccess = mock(TenantAccessService.class);
        Organization tenantA = organization(7, "tenant-a");
        Organization tenantB = organization(8, "tenant-b");
        User user = User.builder().id(11).role(Role.CITIZEN).organization(tenantA).build();
        when(rbac.getCurrentUserOrThrow()).thenReturn(user);
        when(tenantAccess.getCurrentOrganizationEntityOrThrow()).thenReturn(tenantB);

        AtomicReference<ToolExecutionContext> received = new AtomicReference<>();
        ToolExecutor executor = new ToolExecutor(List.of(capturingTool(received)), rbac, tenantAccess, new ObjectMapper());

        String result = executor.execute("test_tool", Map.of(), "question");

        assertTrue(result.contains("FORBIDDEN"));
        assertNull(received.get());
    }

    @Test
    void rejectsAnonymousToolInvocationWithoutLeakingException() {
        RbacService rbac = mock(RbacService.class);
        TenantAccessService tenantAccess = mock(TenantAccessService.class);
        when(rbac.getCurrentUserOrThrow()).thenThrow(new AccessDeniedException("sensitive details"));
        ToolExecutor executor = new ToolExecutor(List.of(capturingTool(new AtomicReference<>())), rbac, tenantAccess, new ObjectMapper());

        String result = executor.execute("test_tool", Map.of(), "question");

        assertTrue(result.contains("FORBIDDEN"));
        assertFalse(result.contains("sensitive details"));
        verifyNoInteractions(tenantAccess);
    }

    private ToolDefinition capturingTool(AtomicReference<ToolExecutionContext> received) {
        return new ToolDefinition() {
            public String getName() { return "test_tool"; }
            public String getDescription() { return "test"; }
            public Map<String, Object> getInputSchema() { return Map.of("type", "object"); }
            public String execute(Map<String, Object> input, ToolExecutionContext context) {
                received.set(context);
                return "{\"ok\":true}";
            }
        };
    }

    private Organization organization(Integer id, String slug) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setSlug(slug);
        return organization;
    }
}
