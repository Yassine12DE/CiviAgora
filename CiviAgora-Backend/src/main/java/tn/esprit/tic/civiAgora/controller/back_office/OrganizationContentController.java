package tn.esprit.tic.civiAgora.controller.back_office;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationContentType;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentDto;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentInteractionRequest;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentRequest;
import tn.esprit.tic.civiAgora.service.OrganizationContentService;
import tn.esprit.tic.civiAgora.service.RbacService;

import java.util.List;

@RestController
@RequestMapping("/org/{organizationId}/content/{type}")
@RequiredArgsConstructor
public class OrganizationContentController {

    private final OrganizationContentService contentService;
    private final RbacService rbacService;

    @GetMapping
    public ResponseEntity<List<OrganizationContentDto>> listContent(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("type") String type
    ) {
        rbacService.requireTenantContentAccess(organizationId);
        OrganizationContentType contentType = OrganizationContentType.fromPath(type);
        User actor = rbacService.getCurrentUserOrThrow();
        return ResponseEntity.ok(contentService.getVisibleContentForCurrentUser(organizationId, contentType, actor));
    }

    @PostMapping
    public ResponseEntity<OrganizationContentDto> createContent(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("type") String type,
            @RequestBody OrganizationContentRequest request
    ) {
        rbacService.requireTenantContentCreationAccess(organizationId);
        OrganizationContentType contentType = OrganizationContentType.fromPath(type);
        User actor = rbacService.getCurrentUserOrThrow();
        OrganizationContentDto created = contentService.createContent(organizationId, contentType, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{contentId}/response")
    public ResponseEntity<OrganizationContentDto> saveContentResponse(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("type") String type,
            @PathVariable("contentId") Long contentId,
            @RequestBody OrganizationContentInteractionRequest request
    ) {
        rbacService.requireTenantContentInteractionAccess(organizationId);
        OrganizationContentType contentType = OrganizationContentType.fromPath(type);
        User actor = rbacService.getCurrentUserOrThrow();
        return ResponseEntity.ok(
                contentService.saveCurrentUserResponse(organizationId, contentType, contentId, request, actor)
        );
    }
}
