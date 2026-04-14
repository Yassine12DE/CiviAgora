package tn.esprit.tic.civiAgora.controller.publicControllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dto.moduleDto.ModuleDto;
import tn.esprit.tic.civiAgora.dto.organizationDto.OrganizationDto;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationAccessRequestCreateDto;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestDto;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.PaymentSummaryDto;
import tn.esprit.tic.civiAgora.mappers.moduleMappers.ModuleMapper;
import tn.esprit.tic.civiAgora.service.ModuleService;
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

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private ModuleMapper moduleMapper;

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
            @Valid @RequestBody OrganizationAccessRequestCreateDto requestDto
    ) {
        OrganizationRequestDto saved = organizationRequestService.createAccessRequest(requestDto);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/organization-requests/payment/{token}")
    public ResponseEntity<PaymentSummaryDto> getPaymentSummary(@PathVariable("token") String token) {
        return ResponseEntity.ok(organizationRequestService.getPaymentSummaryByToken(token));
    }

    @PostMapping("/organization-requests/payment/{token}/success")
    public ResponseEntity<PaymentSummaryDto> completePayment(@PathVariable("token") String token) {
        return ResponseEntity.ok(organizationRequestService.completePaymentByToken(token));
    }

    @GetMapping("/modules")
    public ResponseEntity<List<ModuleDto>> getPublicModules() {
        return ResponseEntity.ok(
                moduleService.getAllModules()
                        .stream()
                        .filter(module -> Boolean.TRUE.equals(module.getActive()))
                        .map(moduleMapper::toDto)
                        .toList()
        );
    }
}
