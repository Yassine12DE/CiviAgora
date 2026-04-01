package tn.esprit.tic.civiAgora.dto.moduleDto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationModuleDto {
    private Long id;
    private Integer organizationId;
    private Long moduleId;
    private String moduleCode;
    private String moduleName;
    private String moduleDescription;
    private Boolean grantedBySaas;
    private Boolean enabledByOrganization;
    private Integer displayOrder;
}