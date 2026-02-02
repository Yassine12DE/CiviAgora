package tn.esprit.tic.civiAgora.dto.organizationDto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationDto {
    private Integer id;
    private String name;
    private String slug;
    private String status;
    private String createdAt;   // KEEP AS STRING
    private int usersCount;
    private int processesCount;
    private String email;
    private String phone;
    private String address;
    private String description;
}
