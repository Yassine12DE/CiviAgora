package tn.esprit.tic.civiAgora.dto.saasDto;

import lombok.Data;

@Data
public class SaasModuleUpsertRequest {
    private String code;
    private String name;
    private String description;
    private String scope;
    private Boolean active;
}
