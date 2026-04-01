package tn.esprit.tic.civiAgora.dto.moduleDto;

import lombok.*;

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
    private Boolean active;
}