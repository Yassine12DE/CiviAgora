package tn.esprit.tic.civiAgora.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRequestRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationContentItemRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationContentResponseRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.UserRepository;
import tn.esprit.tic.civiAgora.dto.analyticsDto.AnalyticsDashboardDto;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationAnalyticsServiceTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private OrganizationModuleRepository organizationModuleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrganizationContentItemRepository contentItemRepository;
    @Mock
    private OrganizationContentResponseRepository contentResponseRepository;
    @Mock
    private ModuleRequestRepository moduleRequestRepository;

    private OrganizationAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationAnalyticsService(
                tenantAccessService,
                organizationModuleRepository,
                userRepository,
                contentItemRepository,
                contentResponseRepository,
                moduleRequestRepository
        );
    }

    @Test
    void getDashboard_throwsForbidden_whenAnalyticsModuleIsNotEnabled() {
        Integer organizationId = 12;
        when(tenantAccessService.assertOrganizationAccessOrThrow(organizationId))
                .thenReturn(organization(organizationId, "city-test"));
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(organizationId, "ANALYTICS"))
                .thenReturn(Optional.empty());

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> service.getDashboard(organizationId)
        );

        assertEquals("Analytics module is not enabled for this organization", exception.getMessage());
    }

    @Test
    void getDashboard_returnsZeroSafeMetrics_whenAnalyticsEnabledAndNoData() {
        Integer organizationId = 7;
        Module analyticsModule = Module.builder()
                .id(1L)
                .code("ANALYTICS")
                .name("Analytics")
                .active(true)
                .build();
        OrganizationModule organizationModule = OrganizationModule.builder()
                .organization(organization(organizationId, "municipality-lake"))
                .module(analyticsModule)
                .grantedBySaas(true)
                .enabledByOrganization(true)
                .build();

        when(tenantAccessService.assertOrganizationAccessOrThrow(organizationId))
                .thenReturn(organization(organizationId, "municipality-lake"));
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(organizationId, "ANALYTICS"))
                .thenReturn(Optional.of(organizationModule));
        when(organizationModuleRepository
                .findByOrganizationIdAndGrantedBySaasTrueAndEnabledByOrganizationTrueOrderByDisplayOrderAsc(organizationId))
                .thenReturn(List.of(organizationModule));
        when(userRepository.findByOrganizationId(organizationId)).thenReturn(List.of());
        when(contentItemRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)).thenReturn(List.of());
        when(contentResponseRepository.findByOrganizationId(organizationId)).thenReturn(List.of());
        when(moduleRequestRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

        AnalyticsDashboardDto dashboard = service.getDashboard(organizationId);

        assertNotNull(dashboard);
        assertTrue(Boolean.TRUE.equals(dashboard.getAnalyticsEnabled()));
        assertEquals("ENABLED", dashboard.getStatus());
        assertTrue(dashboard.getKpis().stream().anyMatch(kpi ->
                "total_users".equals(kpi.getKey()) && "0".equals(kpi.getValueDisplay())
        ));
        assertTrue(dashboard.getCharts().stream().anyMatch(chart -> "user-growth".equals(chart.getKey())));
    }

    private Organization organization(Integer id, String slug) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setSlug(slug);
        organization.setName("Test org");
        return organization;
    }
}
