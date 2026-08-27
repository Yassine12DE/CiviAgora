package tn.esprit.tic.civiAgora.controller.saas;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaasOrganizationModuleControllerTest {

    @Test
    void grantModuleReturnsSuccessfulResponseWithPersistedModules() {
        OrganizationModuleService service = mock(OrganizationModuleService.class);
        SaasOrganizationModuleController controller = new SaasOrganizationModuleController(service);
        OrganizationModuleDto granted = OrganizationModuleDto.builder()
                .organizationId(5)
                .moduleId(1L)
                .moduleCode("VOTE")
                .grantedBySaas(true)
                .build();
        when(service.addModuleToOrganization(5, "1", null)).thenReturn(List.of(granted));

        ResponseEntity<List<OrganizationModuleDto>> response = controller.grantModule(5, "1", null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of(granted), response.getBody());
    }
}
