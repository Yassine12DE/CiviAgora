package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.dto.surveyDto.SurveyDto;
import tn.esprit.tic.civiAgora.mappers.organizationMappers.UserToOrganizationMapper;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.service.SurveyService;
import tn.esprit.tic.civiAgora.service.UserService;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CopilotReadToolsTest {

    @Test
    void citizenCannotQueryOrganizationUsers() {
        RbacService rbac = mock(RbacService.class);
        UserService userService = mock(UserService.class);
        Organization tenant = organization(4, "tenant-a");
        User citizen = User.builder().id(10).role(Role.CITIZEN).organization(tenant).build();
        doThrow(new AccessDeniedException("denied")).when(rbac).requireTenantUserManagementAccess(4);
        OrganizationUserTool tool = new OrganizationUserTool(
                new ObjectMapper(), rbac, userService, new UserToOrganizationMapper());

        assertThrows(AccessDeniedException.class,
                () -> tool.execute(Map.of("action", "get_summary"), new ToolExecutionContext(citizen, tenant, "question")));
        verifyNoInteractions(userService);
    }

    @Test
    void surveyQueriesUseTrustedTenantEvenWhenModelSuppliesAnotherId() throws Exception {
        SurveyService surveyService = mock(SurveyService.class);
        RbacService rbac = mock(RbacService.class);
        Organization tenant = organization(4, "tenant-a");
        User citizen = User.builder().id(10).role(Role.CITIZEN).organization(tenant).build();
        SurveyDto survey = SurveyDto.builder()
                .id(5L).title("Mobility").status("PUBLISHED").lifecycle("OPEN")
                .acceptingResponses(true).submittedByMe(false).build();
        when(surveyService.listForUser(4, citizen)).thenReturn(List.of(survey));
        SurveyTool tool = new SurveyTool(new ObjectMapper(), surveyService, rbac);

        String result = tool.execute(
                Map.of("action", "list_open", "organizationId", 999),
                new ToolExecutionContext(citizen, tenant, "question"));

        assertTrue(result.contains("Mobility"));
        verify(surveyService).listForUser(4, citizen);
        verify(surveyService, never()).listForUser(eq(999), any());
    }

    private Organization organization(Integer id, String slug) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setSlug(slug);
        return organization;
    }
}
