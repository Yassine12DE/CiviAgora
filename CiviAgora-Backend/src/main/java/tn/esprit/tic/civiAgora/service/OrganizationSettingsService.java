package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationSettings;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationSettingsRepository;
import tn.esprit.tic.civiAgora.dto.organizationSettingsDto.OrganizationSettingsDto;
import tn.esprit.tic.civiAgora.mappers.organizationSettingsMappers.OrganizationSettingsMapper;

@Service
@RequiredArgsConstructor
public class OrganizationSettingsService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsRepository organizationSettingsRepository;
    private final OrganizationSettingsMapper organizationSettingsMapper;
    private final TenantAccessService tenantAccessService;

    public OrganizationSettingsDto getSettingsByOrganizationId(Integer organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        OrganizationSettings settings = organizationSettingsRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultSettingsForOrganization(organization));

        return organizationSettingsMapper.toDto(settings);
    }

    public OrganizationSettingsDto getCurrentOrganizationSettings() {
        Organization organization = tenantAccessService.getResolvedOrganizationOrThrow();
        return getSettingsByOrganizationId(organization.getId());
    }

    public OrganizationSettingsDto getTenantSettings(Integer organizationId) {
        tenantAccessService.assertOrganizationAccessOrThrow(organizationId);
        return getSettingsByOrganizationId(organizationId);
    }

    public OrganizationSettings createDefaultSettingsForOrganization(Organization organization) {
        return createDefaultSettingsForOrganization(organization, null, null);
    }

    public OrganizationSettings createDefaultSettingsForOrganization(
            Organization organization,
            String requestedPrimaryColor,
            String requestedSecondaryColor
    ) {
        OrganizationSettings settings = OrganizationSettings.builder()
                .organization(organization)
                .logoUrl(null)
                .primaryColor(isHexColor(requestedPrimaryColor) ? requestedPrimaryColor : "#7B2CBF")
                .secondaryColor(isHexColor(requestedSecondaryColor) ? requestedSecondaryColor : "#FF6B35")
                .homeTitle("Welcome to " + organization.getName())
                .welcomeText("This is the front-office of " + organization.getName())
                .bannerImageUrl(null)
                .footerText(organization.getName() + " Footer")
                .build();

        return organizationSettingsRepository.save(settings);
    }

    private boolean isHexColor(String value) {
        return value != null && value.matches("^#[0-9a-fA-F]{6}$");
    }

    public OrganizationSettingsDto updateSettings(Integer organizationId, OrganizationSettingsDto updatedSettingsDto) {
        OrganizationSettings existing = organizationSettingsRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization settings not found"));

        if (existing.getOrganization() == null) {
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            existing.setOrganization(organization);
        }

        existing.setLogoUrl(updatedSettingsDto.getLogoUrl());
        existing.setPrimaryColor(updatedSettingsDto.getPrimaryColor());
        existing.setSecondaryColor(updatedSettingsDto.getSecondaryColor());
        existing.setHomeTitle(updatedSettingsDto.getHomeTitle());
        existing.setWelcomeText(updatedSettingsDto.getWelcomeText());
        existing.setBannerImageUrl(updatedSettingsDto.getBannerImageUrl());
        existing.setFooterText(updatedSettingsDto.getFooterText());

        return organizationSettingsMapper.toDto(organizationSettingsRepository.save(existing));
    }

    public OrganizationSettingsDto updateTenantSettings(Integer organizationId, OrganizationSettingsDto updatedSettingsDto) {
        tenantAccessService.assertOrganizationAccessOrThrow(organizationId);
        return updateSettings(organizationId, updatedSettingsDto);
    }
}
