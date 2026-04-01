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

    public OrganizationSettingsDto getSettingsByOrganizationId(Integer organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        OrganizationSettings settings = organizationSettingsRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultSettingsForOrganization(organization));

        return organizationSettingsMapper.toDto(settings);
    }

    public OrganizationSettings createDefaultSettingsForOrganization(Organization organization) {
        OrganizationSettings settings = OrganizationSettings.builder()
                .organization(organization)
                .logoUrl(null)
                .primaryColor("#2563eb")
                .secondaryColor("#7c3aed")
                .homeTitle("Welcome to " + organization.getName())
                .welcomeText("This is the front-office of " + organization.getName())
                .bannerImageUrl(null)
                .footerText(organization.getName() + " Footer")
                .build();

        return organizationSettingsRepository.save(settings);
    }

    public OrganizationSettingsDto updateSettings(Integer organizationId, OrganizationSettingsDto updatedSettingsDto) {
        OrganizationSettings existing = organizationSettingsRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization settings not found"));

        existing.setLogoUrl(updatedSettingsDto.getLogoUrl());
        existing.setPrimaryColor(updatedSettingsDto.getPrimaryColor());
        existing.setSecondaryColor(updatedSettingsDto.getSecondaryColor());
        existing.setHomeTitle(updatedSettingsDto.getHomeTitle());
        existing.setWelcomeText(updatedSettingsDto.getWelcomeText());
        existing.setBannerImageUrl(updatedSettingsDto.getBannerImageUrl());
        existing.setFooterText(updatedSettingsDto.getFooterText());

        return organizationSettingsMapper.toDto(organizationSettingsRepository.save(existing));
    }
}