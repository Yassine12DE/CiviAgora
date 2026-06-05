package tn.esprit.tic.civiAgora.dto.saasDto;

import lombok.Data;

@Data
public class SaasModuleCatalogItemDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String scope;
    private Boolean active;
    private long organizationsUsing;
    private long totalOrganizations;
}
