package tn.esprit.tic.civiAgora.service;

import org.junit.jupiter.api.Test;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.ModuleRequest;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleRequestStatus;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRequestRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dto.moduleRequestDto.ModuleRequestDto;
import tn.esprit.tic.civiAgora.mappers.moduleMappers.OrganizationModuleMapper;
import tn.esprit.tic.civiAgora.mappers.moduleRequestMappers.ModuleRequestMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModuleRequestServiceTest {

    @Test
    void approveRequestPersistsApprovalAndGrantWhenNotificationFails() throws Exception {
        ModuleRequestRepository moduleRequestRepository = mock(ModuleRequestRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        OrganizationModuleRepository organizationModuleRepository = mock(OrganizationModuleRepository.class);
        ModuleService moduleService = mock(ModuleService.class);
        TenantAccessService tenantAccessService = mock(TenantAccessService.class);
        EmailService emailService = mock(EmailService.class);
        ModuleNotificationEmailService notificationService = new ModuleNotificationEmailService(emailService);
        OrganizationModuleService organizationModuleService = new OrganizationModuleService(
                organizationModuleRepository,
                organizationRepository,
                moduleService,
                new OrganizationModuleMapper(),
                tenantAccessService,
                notificationService,
                new OrganizationSubscriptionAccessPolicy()
        );
        ModuleRequestService service = new ModuleRequestService(
                moduleRequestRepository,
                organizationRepository,
                moduleService,
                organizationModuleService,
                new ModuleRequestMapper(),
                tenantAccessService,
                notificationService
        );

        Organization organization = new Organization();
        organization.setId(5);
        organization.setName("Municipality of Tunis");
        organization.setSlug("tunisie");
        organization.setEmail("contact@tunis.com");

        Module module = Module.builder()
                .id(1L)
                .code("VOTE")
                .name("Voting")
                .active(true)
                .build();
        ModuleRequest request = ModuleRequest.builder()
                .id(9L)
                .organization(organization)
                .module(module)
                .status(ModuleRequestStatus.PENDING)
                .requestDate(LocalDateTime.now().minusDays(1))
                .build();

        when(moduleRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(moduleService.getModuleByCode(module.getCode())).thenReturn(module);
        when(organizationModuleRepository.findByOrganizationIdAndModuleId(organization.getId(), module.getId()))
                .thenReturn(Optional.empty());
        when(organizationModuleRepository.findByOrganizationIdAndGrantedBySaasTrue(organization.getId()))
                .thenReturn(List.of());
        when(organizationModuleRepository.save(any(OrganizationModule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(moduleRequestRepository.save(request)).thenReturn(request);
        doThrow(new IllegalStateException("SMTP timeout"))
                .when(emailService).sendHtmlMessage(anyString(), anyString(), anyString());

        ModuleRequestDto result = assertDoesNotThrow(
                () -> service.approveRequest(request.getId(), "Approved by SaaS admin")
        );

        verify(organizationModuleRepository).save(any(OrganizationModule.class));
        verify(moduleRequestRepository).save(request);
        verify(emailService).sendHtmlMessage(anyString(), anyString(), anyString());
        assertEquals(ModuleRequestStatus.APPROVED, request.getStatus());
        assertNotNull(request.getReviewedDate());
        assertEquals("APPROVED", result.getStatus());
    }
}
