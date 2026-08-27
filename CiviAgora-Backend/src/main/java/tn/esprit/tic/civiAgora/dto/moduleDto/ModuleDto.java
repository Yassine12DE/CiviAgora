package tn.esprit.tic.civiAgora.dto.moduleDto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleDto {
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
}
