package tn.esprit.tic.civiAgora.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationStatus;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.mappers.moduleMappers.OrganizationModuleMapper;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationModuleServiceTest {

    @Mock
    private OrganizationModuleRepository organizationModuleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ModuleService moduleService;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private ModuleNotificationEmailService moduleNotificationEmailService;

    private OrganizationModuleService organizationModuleService;

    @BeforeEach
    void setUp() {
        organizationModuleService = new OrganizationModuleService(
                organizationModuleRepository,
                organizationRepository,
                moduleService,
                new OrganizationModuleMapper(),
                tenantAccessService,
                moduleNotificationEmailService,
                new OrganizationSubscriptionAccessPolicy()
        );
    }

    @Test
    void addModuleToOrganizationCreatesNewGrant() {
        Organization organization = organization(7);
        Module module = module(3L, "VOTE", "Voting");

        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(moduleService.getModuleById(module.getId())).thenReturn(module);
        when(organizationModuleRepository.findByOrganizationIdAndModuleId(organization.getId(), module.getId()))
                .thenReturn(Optional.empty());
        when(organizationModuleRepository.findByOrganizationIdAndGrantedBySaasTrue(organization.getId()))
                .thenReturn(List.of(), List.of(grantedModule(organization, module, 1)));
        when(organizationModuleRepository.save(any(OrganizationModule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<OrganizationModuleDto> result =
                organizationModuleService.addModuleToOrganization(organization.getId(), String.valueOf(module.getId()), null);

        ArgumentCaptor<OrganizationModule> savedCaptor = ArgumentCaptor.forClass(OrganizationModule.class);
        verify(organizationModuleRepository).save(savedCaptor.capture());

        OrganizationModule saved = savedCaptor.getValue();
        assertTrue(saved.getGrantedBySaas());
        assertTrue(saved.getEnabledByOrganization());
        assertEquals(1, saved.getDisplayOrder());
        assertEquals(organization.getId(), saved.getOrganization().getId());
        assertEquals(module.getId(), saved.getModule().getId());

        assertEquals(1, result.size());
        assertEquals("VOTE", result.get(0).getModuleCode());
        assertEquals("Voting", result.get(0).getModuleName());
        verify(moduleNotificationEmailService).sendModuleGrantedNotificationAfterCommit(
                organization,
                module.getName(),
                module.getCode()
        );
    }

    @Test
    void addModuleToOrganizationPersistsGrantWhenNotificationFails() throws Exception {
        Organization organization = organization(7);
        Module module = module(3L, "VOTE", "Voting");
        OrganizationModule granted = grantedModule(organization, module, 1);
        EmailService emailService = mock(EmailService.class);
        ModuleNotificationEmailService resilientNotificationService =
                new ModuleNotificationEmailService(emailService);
        OrganizationModuleService service = new OrganizationModuleService(
                organizationModuleRepository,
                organizationRepository,
                moduleService,
                new OrganizationModuleMapper(),
                tenantAccessService,
                resilientNotificationService,
                new OrganizationSubscriptionAccessPolicy()
        );

        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(moduleService.getModuleById(module.getId())).thenReturn(module);
        when(organizationModuleRepository.findByOrganizationIdAndModuleId(organization.getId(), module.getId()))
                .thenReturn(Optional.empty());
        when(organizationModuleRepository.findByOrganizationIdAndGrantedBySaasTrue(organization.getId()))
                .thenReturn(List.of(), List.of(granted));
        when(organizationModuleRepository.save(any(OrganizationModule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService).sendHtmlMessage(anyString(), anyString(), anyString());

        List<OrganizationModuleDto> result = assertDoesNotThrow(
                () -> service.addModuleToOrganization(
                        organization.getId(),
                        String.valueOf(module.getId()),
                        null
                )
        );

        verify(organizationModuleRepository).save(any(OrganizationModule.class));
        verify(emailService).sendHtmlMessage(anyString(), anyString(), anyString());
        assertEquals(1, result.size());
        assertEquals("VOTE", result.get(0).getModuleCode());
    }

    @Test
    void addModuleToOrganizationRejectsDuplicateGrant() {
        Organization organization = organization(7);
        Module module = module(3L, "VOTE", "Voting");
        OrganizationModule existing = grantedModule(organization, module, 4);

        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(moduleService.getModuleById(module.getId())).thenReturn(module);
        when(organizationModuleRepository.findByOrganizationIdAndModuleId(organization.getId(), module.getId()))
                .thenReturn(Optional.of(existing));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> organizationModuleService.addModuleToOrganization(
                        organization.getId(),
                        String.valueOf(module.getId()),
                        null
                )
        );

        assertEquals("Module is already assigned to this organization", exception.getMessage());
        verify(organizationModuleRepository, never()).save(any(OrganizationModule.class));
    }

    @Test
    void addModuleToOrganizationRevivesSoftDeletedGrant() {
        Organization organization = organization(7);
        Module module = module(3L, "VOTE", "Voting");
        OrganizationModule existing = grantedModule(organization, module, 9);
        existing.setGrantedBySaas(false);
        existing.setEnabledByOrganization(false);

        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(moduleService.getModuleById(module.getId())).thenReturn(module);
        when(organizationModuleRepository.findByOrganizationIdAndModuleId(organization.getId(), module.getId()))
                .thenReturn(Optional.of(existing));
        when(organizationModuleRepository.findByOrganizationIdAndGrantedBySaasTrue(organization.getId()))
                .thenReturn(List.of(existing));
        when(organizationModuleRepository.save(any(OrganizationModule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<OrganizationModuleDto> result =
                organizationModuleService.addModuleToOrganization(organization.getId(), String.valueOf(module.getId()), null);

        verify(organizationModuleRepository, times(1)).save(existing);
        assertTrue(existing.getGrantedBySaas());
        assertTrue(existing.getEnabledByOrganization());
        assertEquals(1, result.size());
    }

    @Test
    void removeModuleFromOrganizationDeletesExistingGrant() {
        Organization organization = organization(7);
        Module module = module(3L, "VOTE", "Voting");
        OrganizationModule existing = grantedModule(organization, module, 2);

        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(moduleService.getModuleById(module.getId())).thenReturn(module);
        when(organizationModuleRepository.findByOrganizationIdAndModuleIdAndGrantedBySaasTrue(
                organization.getId(),
                module.getId()
        )).thenReturn(Optional.of(existing));
        when(organizationModuleRepository.findByOrganizationIdAndGrantedBySaasTrue(organization.getId()))
                .thenReturn(List.of());

        List<OrganizationModuleDto> result = organizationModuleService.removeModuleFromOrganization(
                organization.getId(),
                String.valueOf(module.getId())
        );

        verify(organizationModuleRepository).delete(existing);
        assertTrue(result.isEmpty());
    }

    @Test
    void removeModuleFromOrganizationThrowsWhenGrantMissing() {
        Organization organization = organization(7);
        Module module = module(3L, "VOTE", "Voting");

        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(moduleService.getModuleById(module.getId())).thenReturn(module);
        when(organizationModuleRepository.findByOrganizationIdAndModuleIdAndGrantedBySaasTrue(
                organization.getId(),
                module.getId()
        )).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> organizationModuleService.removeModuleFromOrganization(
                        organization.getId(),
                        String.valueOf(module.getId())
                )
        );

        assertEquals("Module is not assigned to this organization", exception.getMessage());
        verify(organizationModuleRepository, never()).delete(any(OrganizationModule.class));
    }

    @Test
    void getTenantModulesReturnsGrantForLegacyActiveOrganization() {
        Organization organization = organization(7);
        organization.setStatus(OrganizationStatus.ACTIVE);
        Module module = module(3L, "VOTE", "Voting");
        OrganizationModule grant = grantedModule(organization, module, 1);

        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(organizationModuleRepository.findByOrganizationIdAndGrantedBySaasTrue(organization.getId()))
                .thenReturn(List.of(grant));

        List<OrganizationModuleDto> result = organizationModuleService.getTenantModules(organization.getId());

        assertEquals(1, result.size());
        assertEquals("VOTE", result.get(0).getModuleCode());
    }

    @Test
    void grantModuleToOrganizationIsIdempotentAndDoesNotSendDuplicateNotification() {
        Organization organization = organization(7);
        Module module = module(3L, "VOTE", "Voting");
        OrganizationModule existing = grantedModule(organization, module, 1);

        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(moduleService.getModuleByCode(module.getCode())).thenReturn(module);
        when(organizationModuleRepository.findByOrganizationIdAndModuleId(organization.getId(), module.getId()))
                .thenReturn(Optional.of(existing));
        when(organizationModuleRepository.save(existing)).thenReturn(existing);

        OrganizationModuleDto result = organizationModuleService.grantModuleToOrganization(
                organization.getId(),
                module.getCode(),
                null
        );

        verify(organizationModuleRepository).save(existing);
        verify(moduleNotificationEmailService, never()).sendModuleGrantedNotificationAfterCommit(
                any(),
                anyString(),
                anyString()
        );
        assertEquals("VOTE", result.getModuleCode());
    }

    private Organization organization(Integer id) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName("Test Org");
        organization.setSlug("test-org");
        organization.setEmail("contact@test-org.example");
        return organization;
    }

    private Module module(Long id, String code, String name) {
        Module module = new Module();
        module.setId(id);
        module.setCode(code);
        module.setName(name);
        module.setActive(true);
        return module;
    }

    private OrganizationModule grantedModule(Organization organization, Module module, Integer displayOrder) {
        OrganizationModule organizationModule = new OrganizationModule();
        organizationModule.setOrganization(organization);
        organizationModule.setModule(module);
        organizationModule.setGrantedBySaas(true);
        organizationModule.setEnabledByOrganization(true);
        organizationModule.setDisplayOrder(displayOrder);
        return organizationModule;
    }
}
