package tn.esprit.tic.civiAgora.dto.organizationRequestDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationAccessRequestCreateDto {

    @NotBlank(message = "Organization name is required")
    @Size(max = 160, message = "Organization name must be 160 characters or less")
    private String organizationName;

    @NotBlank(message = "Preferred slug is required")
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9-]{1,58}[a-zA-Z0-9]$", message = "Slug must contain 3 to 60 letters, numbers, or hyphens")
    private String desiredSlug;

    @NotBlank(message = "Contact person name is required")
    @Size(max = 160, message = "Contact person name must be 160 characters or less")
    private String contactPersonName;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be valid")
    private String contactEmail;

    @Size(max = 50, message = "Phone must be 50 characters or less")
    private String phone;

    @Size(max = 255, message = "Address must be 255 characters or less")
    private String address;

    @Size(max = 2048, message = "Description must be 2048 characters or less")
    private String description;

    private String logoUrl;

    @NotBlank(message = "Admin first name is required")
    @Size(max = 80, message = "Admin first name must be 80 characters or less")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    @Size(max = 80, message = "Admin last name must be 80 characters or less")
    private String adminLastName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be valid")
    private String adminEmail;

    @NotBlank(message = "Temporary admin password is required")
    @Size(min = 8, max = 72, message = "Temporary admin password must be between 8 and 72 characters")
    private String adminTemporaryPassword;

    @NotNull(message = "Expected number of users is required")
    @Min(value = 1, message = "Expected number of users must be at least 1")
    private Integer expectedNumberOfUsers;

    private List<String> requestedModuleCodes;

    @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Primary color must be a valid hex color")
    private String requestedPrimaryColor;

    @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Secondary color must be a valid hex color")
    private String requestedSecondaryColor;

    @Size(max = 1200, message = "Branding notes must be 1200 characters or less")
    private String brandingNotes;

    @Size(max = 2048, message = "Additional notes must be 2048 characters or less")
    private String additionalNotes;

    private Boolean publicVisibilityRequested;
}
