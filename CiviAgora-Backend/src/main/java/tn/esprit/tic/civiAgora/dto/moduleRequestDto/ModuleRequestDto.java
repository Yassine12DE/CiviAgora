package tn.esprit.tic.civiAgora.dto.moduleRequestDto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleRequestDto {
    private Long id;
    private Integer organizationId;
    private String organizationName;
    private Long moduleId;
    private String moduleCode;
    private String moduleName;
    private String status;
    private LocalDateTime requestDate;
    private LocalDateTime reviewedDate;
    private String comment;
}