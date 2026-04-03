package tn.esprit.tic.civiAgora.controller.publicControllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.dto.organizationDto.OrganizationDto;
import tn.esprit.tic.civiAgora.dto.organizationSettingsDto.OrganizationSettingsDto;
import tn.esprit.tic.civiAgora.service.OrganizationModuleService;
import tn.esprit.tic.civiAgora.service.OrganizationService;
import tn.esprit.tic.civiAgora.service.OrganizationSettingsService;

import java.util.List;

@RestController
@RequestMapping("/public/organization")
@RequiredArgsConstructor
public class PublicOrganizationController {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsService organizationSettingsService;
    private final OrganizationModuleService organizationModuleService;
    private final OrganizationService organizationService;

    private String extractSlugFromHost(HttpServletRequest request) {
        String host = request.getServerName();

        if (host == null || host.isBlank()) {
            throw new RuntimeException("Host is missing");
        }

        if (host.equals("lvh.me") || host.equals("localhost") || host.equals("127.0.0.1")) {
            throw new RuntimeException("No tenant slug in host");
        }

        if (host.endsWith(".lvh.me")) {
            return host.split("\\.")[0];
        }

        throw new RuntimeException("Unsupported host: " + host);
    }

    private Organization resolveOrganization(HttpServletRequest request) {
        String slug = extractSlugFromHost(request);

        return organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Organization not found with slug: " + slug));
    }

    @GetMapping
    public ResponseEntity<OrganizationDto> getCurrentOrganization(HttpServletRequest request) {
        String slug = extractSlugFromHost(request);
        return ResponseEntity.ok(organizationService.getOrganizationDtoBySlug(slug));
    }

    @GetMapping("/settings")
    public ResponseEntity<OrganizationSettingsDto> getOrganizationSettings(HttpServletRequest request) {
        Organization organization = resolveOrganization(request);
        return ResponseEntity.ok(
                organizationSettingsService.getSettingsByOrganizationId(organization.getId())
        );
    }

    @GetMapping("/modules")
    public ResponseEntity<List<OrganizationModuleDto>> getVisibleModules(HttpServletRequest request) {
        Organization organization = resolveOrganization(request);
        return ResponseEntity.ok(
                organizationModuleService.getVisibleModulesForOrganization(organization.getId())
        );
    }
}
