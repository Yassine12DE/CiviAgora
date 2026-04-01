package tn.esprit.tic.civiAgora.mappers.organizationSettingsMappers;

import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationSettings;
import tn.esprit.tic.civiAgora.dto.organizationSettingsDto.OrganizationSettingsDto;

@Component
public class OrganizationSettingsMapper {

    public OrganizationSettingsDto toDto(OrganizationSettings entity) {
        if (entity == null) return null;

        return OrganizationSettingsDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization().getId())
                .logoUrl(entity.getLogoUrl())
                .primaryColor(entity.getPrimaryColor())
                .secondaryColor(entity.getSecondaryColor())
                .homeTitle(entity.getHomeTitle())
                .welcomeText(entity.getWelcomeText())
                .bannerImageUrl(entity.getBannerImageUrl())
                .footerText(entity.getFooterText())
                .build();
    }

    public OrganizationSettings toEntity(OrganizationSettingsDto dto) {
        if (dto == null) return null;

        return OrganizationSettings.builder()
                .logoUrl(dto.getLogoUrl())
                .primaryColor(dto.getPrimaryColor())
                .secondaryColor(dto.getSecondaryColor())
                .homeTitle(dto.getHomeTitle())
                .welcomeText(dto.getWelcomeText())
                .bannerImageUrl(dto.getBannerImageUrl())
                .footerText(dto.getFooterText())
                .build();
    }
}