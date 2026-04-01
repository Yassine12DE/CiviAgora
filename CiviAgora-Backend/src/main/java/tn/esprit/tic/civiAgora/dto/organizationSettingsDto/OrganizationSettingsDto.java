package tn.esprit.tic.civiAgora.dto.organizationSettingsDto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSettingsDto {
    private Long id;
    private Integer organizationId;
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String homeTitle;
    private String welcomeText;
    private String bannerImageUrl;
    private String footerText;
}