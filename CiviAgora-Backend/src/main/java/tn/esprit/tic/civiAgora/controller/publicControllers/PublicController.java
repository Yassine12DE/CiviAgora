package tn.esprit.tic.civiAgora.controller.publicControllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dto.organizationDto.OrganizationDto;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestDto;
import tn.esprit.tic.civiAgora.service.OrganizationRequestService;
import tn.esprit.tic.civiAgora.service.OrganizationService;

import java.util.List;

@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationRequestService organizationRequestService;

    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationDto>> getPublicOrganizations() {
        return ResponseEntity.ok(organizationService.getPublicOrganizations());
    }

    @GetMapping("/organizations/{slug}")
    public ResponseEntity<OrganizationDto> getOrganizationBySlug(@PathVariable("slug") String slug) {
        OrganizationDto dto = organizationService.getOrganizationDtoBySlug(slug);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/organization-requests")
    public ResponseEntity<OrganizationRequestDto> submitOrganizationRequest(
            @RequestBody OrganizationRequestDto requestDto
    ) {
        OrganizationRequestDto saved = organizationRequestService.createRequest(requestDto);
        return ResponseEntity.ok(saved);
    }
}
