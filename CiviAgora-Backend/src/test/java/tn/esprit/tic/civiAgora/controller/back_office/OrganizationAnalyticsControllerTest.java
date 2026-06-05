package tn.esprit.tic.civiAgora.controller.back_office;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.tic.civiAgora.dto.analyticsDto.AnalyticsDashboardDto;
import tn.esprit.tic.civiAgora.exception.GlobalExceptionHandler;
import tn.esprit.tic.civiAgora.service.OrganizationAnalyticsService;
import tn.esprit.tic.civiAgora.service.RbacService;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationAnalyticsControllerTest {

    private MockMvc mockMvc;
    private RbacService rbacService;
    private OrganizationAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        rbacService = mock(RbacService.class);
        analyticsService = mock(OrganizationAnalyticsService.class);

        OrganizationAnalyticsController controller = new OrganizationAnalyticsController(
                analyticsService,
                rbacService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void getDashboard_returnsPayloadWhenAccessGranted() throws Exception {
        AnalyticsDashboardDto dto = AnalyticsDashboardDto.builder()
                .organizationId(10)
                .organizationSlug("city-test")
                .analyticsEnabled(true)
                .status("ENABLED")
                .message("ok")
                .kpis(List.of())
                .charts(List.of())
                .moduleActivity(List.of())
                .recentActivities(List.of())
                .insights(List.of())
                .build();
        when(analyticsService.getDashboard(10)).thenReturn(dto);

        mockMvc.perform(get("/org/10/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(10))
                .andExpect(jsonPath("$.analyticsEnabled").value(true));
    }

    @Test
    void getDashboard_returns403WhenAccessDenied() throws Exception {
        doThrow(new AccessDeniedException("Forbidden analytics"))
                .when(rbacService)
                .requireTenantAnalyticsAccess(10);

        mockMvc.perform(get("/org/10/analytics/dashboard"))
                .andExpect(status().isForbidden());
    }
}
