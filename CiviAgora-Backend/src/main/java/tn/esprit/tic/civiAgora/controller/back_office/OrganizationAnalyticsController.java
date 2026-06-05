package tn.esprit.tic.civiAgora.controller.back_office;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.tic.civiAgora.dto.analyticsDto.AnalyticsDashboardDto;
import tn.esprit.tic.civiAgora.service.OrganizationAnalyticsService;
import tn.esprit.tic.civiAgora.service.RbacService;

@RestController
@RequestMapping("/org/{organizationId}/analytics")
@RequiredArgsConstructor
public class OrganizationAnalyticsController {

    private final OrganizationAnalyticsService organizationAnalyticsService;
    private final RbacService rbacService;

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardDto> getDashboard(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantAnalyticsAccess(organizationId);
        return ResponseEntity.ok(organizationAnalyticsService.getDashboard(organizationId));
    }
}
