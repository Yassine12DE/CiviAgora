package tn.esprit.tic.civiAgora.dto.organizationSettingsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicOrganizationBrandingDto {
    private Integer id;
    private String name;
    private String slug;
    private String status;
    private String createdAt;
    private int usersCount;
    private int processesCount;
    private String email;
    private String phone;
    private String address;
    private String description;

    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String homeTitle;
    private String welcomeText;
    private String bannerImageUrl;
    private String footerText;
}