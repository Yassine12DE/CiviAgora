package tn.esprit.tic.civiAgora.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.dto.chatDto.ChatRequest;
import tn.esprit.tic.civiAgora.dto.chatDto.ChatResponse;
import tn.esprit.tic.civiAgora.dto.chatDto.OllamaChatResult;
import tn.esprit.tic.civiAgora.service.ModuleAccessService;
import tn.esprit.tic.civiAgora.service.TenantAccessService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {
    private final OllamaClient ollamaClient = mock(OllamaClient.class);
    private final TenantAccessService tenantAccess = mock(TenantAccessService.class);
    private final ModuleAccessService moduleAccess = mock(ModuleAccessService.class);
    private final ChatUiPayloadFactory uiPayloadFactory = mock(ChatUiPayloadFactory.class);
    private final ChatService service = new ChatService(ollamaClient, tenantAccess, moduleAccess, uiPayloadFactory);

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousChatNeverEnablesToolsOrLoadsPrivateModuleContext() {
        when(tenantAccess.getResolvedOrganizationFromRequestContext()).thenReturn(null);
        when(ollamaClient.chat(anyString(), anyList(), eq(false), eq("What is CIVOX?")))
                .thenReturn(new OllamaChatResult("Public answer"));

        ChatResponse response = service.chat(new ChatRequest("What is CIVOX?", List.of()));

        assertFalse(response.authenticated());
        assertEquals("Public answer", response.reply());
        verifyNoInteractions(moduleAccess);
        verify(ollamaClient).chat(contains("user is anonymous"), anyList(), eq(false), eq("What is CIVOX?"));
    }

    @Test
    void authenticatedCrossTenantContextIsRejectedBeforeOllama() {
        Organization tenantA = organization(1, "a");
        Organization tenantB = organization(2, "b");
        User user = User.builder().id(8).firstName("Eya").role(Role.MANAGER).organization(tenantA).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(tenantAccess.getCurrentOrganizationEntityOrThrow()).thenReturn(tenantB);
        when(tenantAccess.isCurrentUserSuperAdmin()).thenReturn(false);

        ChatResponse response = service.chat(new ChatRequest("List users", List.of()));

        assertTrue(response.authenticated());
        assertTrue(response.reply().contains("verify your organization"));
        verifyNoInteractions(ollamaClient, moduleAccess);
    }

    private Organization organization(Integer id, String slug) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setSlug(slug);
        return organization;
    }
}
