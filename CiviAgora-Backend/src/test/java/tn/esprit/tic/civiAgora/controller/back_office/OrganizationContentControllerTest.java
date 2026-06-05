package tn.esprit.tic.civiAgora.controller.back_office;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.tic.civiAgora.exception.GlobalExceptionHandler;
import tn.esprit.tic.civiAgora.service.OrganizationContentService;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.service.TenantAccessService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationContentControllerTest {

    private MockMvc mockMvc;
    private RbacService rbacService;

    @BeforeEach
    void setUp() {
        OrganizationContentService contentService = mock(OrganizationContentService.class);
        rbacService = mock(RbacService.class);
        TenantAccessService tenantAccessService = mock(TenantAccessService.class);

        OrganizationContentController controller = new OrganizationContentController(
                contentService,
                rbacService,
                tenantAccessService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void getContent_returns400_whenModuleSlugIsInvalid() throws Exception {
        mockMvc.perform(get("/org/5/content/not-a-module"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getContent_returns403_whenTenantAccessDenied() throws Exception {
        doThrow(new AccessDeniedException("Tenant mismatch"))
                .when(rbacService)
                .requireTenantContentAccess(5);

        mockMvc.perform(get("/org/5/content/concertation"))
                .andExpect(status().isForbidden());
    }
}
