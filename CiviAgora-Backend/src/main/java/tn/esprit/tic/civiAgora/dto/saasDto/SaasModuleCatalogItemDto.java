package tn.esprit.tic.civiAgora.dto.saasDto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaasModuleCatalogItemDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String scope;
    private String billingType;
    private BigDecimal oneTimePrice;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;
    private Boolean active;
    private long organizationsUsing;
    private long totalOrganizations;
}
