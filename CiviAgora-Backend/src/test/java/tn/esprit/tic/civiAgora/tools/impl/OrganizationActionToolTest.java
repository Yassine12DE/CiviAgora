package tn.esprit.tic.civiAgora.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationContentType;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentDto;
import tn.esprit.tic.civiAgora.service.*;
import tn.esprit.tic.civiAgora.tools.PendingCopilotActionService;
import tn.esprit.tic.civiAgora.tools.ToolExecutionContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationActionToolTest {

    @Test
    void sensitiveWriteDoesNotExecuteUntilSeparateExplicitConfirmation() throws Exception {
        RbacService rbac = mock(RbacService.class);
        OrganizationContentService contentService = mock(OrganizationContentService.class);
        SurveyService surveyService = mock(SurveyService.class);
        OrganizationModuleService moduleService = mock(OrganizationModuleService.class);
        ModuleService catalogService = mock(ModuleService.class);
        ModuleRequestService moduleRequestService = mock(ModuleRequestService.class);
        UserService userService = mock(UserService.class);
        PendingCopilotActionService confirmations = new PendingCopilotActionService();
        OrganizationActionTool tool = new OrganizationActionTool(
                new ObjectMapper(), confirmations, rbac, contentService, surveyService,
                moduleService, catalogService, moduleRequestService, userService);
        Organization tenant = organization(4, "tenant-a");
        User manager = User.builder().id(10).role(Role.MANAGER).organization(tenant).build();
        OrganizationContentDto target = OrganizationContentDto.builder().id(22L).title("Mobility vote").build();
        OrganizationContentDto updated = OrganizationContentDto.builder().id(22L).title("Mobility vote").published(true).build();
        when(contentService.getContent(4, OrganizationContentType.VOTE)).thenReturn(List.of(target));
        when(contentService.updateContentPublicationStatus(4, OrganizationContentType.VOTE, 22L, true))
                .thenReturn(updated);

        String prepared = tool.execute(Map.of(
                        "action", "prepare_content_publication",
                        "contentType", "VOTE",
                        "contentId", 22,
                        "published", true
                ), new ToolExecutionContext(manager, tenant, "Publish the mobility vote"));

        assertTrue(prepared.contains("confirmationRequired"));
        verify(contentService, never()).updateContentPublicationStatus(anyInt(), any(), anyLong(), anyBoolean());

        assertThrows(PendingCopilotActionService.ConfirmationRequiredException.class,
                () -> tool.execute(Map.of("action", "confirm_pending"),
                        new ToolExecutionContext(manager, tenant, "go ahead")));
        verify(contentService, never()).updateContentPublicationStatus(anyInt(), any(), anyLong(), anyBoolean());

        String executed = tool.execute(Map.of("action", "confirm_pending"),
                new ToolExecutionContext(manager, tenant, "Confirm"));

        assertTrue(executed.contains("\"ok\":true"));
        verify(contentService).updateContentPublicationStatus(4, OrganizationContentType.VOTE, 22L, true);
        assertNull(confirmations.current(new ToolExecutionContext(manager, tenant, "question")));
    }

    private Organization organization(Integer id, String slug) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setSlug(slug);
        return organization;
    }
}
