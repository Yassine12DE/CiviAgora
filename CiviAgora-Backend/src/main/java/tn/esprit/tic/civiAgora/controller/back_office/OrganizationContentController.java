package tn.esprit.tic.civiAgora.controller.back_office;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import tn.esprit.tic.civiAgora.service.TenantAccessService;

import java.util.List;

@RestController
@RequestMapping("/org/{organizationId}/content/{type}")
@RequiredArgsConstructor
@Slf4j
public class OrganizationContentController {

    private final OrganizationContentService contentService;
    private final RbacService rbacService;
    private final TenantAccessService tenantAccessService;

    @GetMapping
    public ResponseEntity<List<OrganizationContentDto>> listContent(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("type") String type
    ) {
        log.debug("Content GET request: pathOrganizationId={}, moduleSlug={}, jwtOrganizationId={}, jwtOrganizationSlug={}, resolvedTenantSlug={}",
                organizationId,
                type,
                tenantAccessService.getCurrentJwtOrganizationId(),
                tenantAccessService.getCurrentJwtOrganizationSlug(),
                tenantAccessService.getResolvedOrganizationFromRequestContext() != null
                        ? tenantAccessService.getResolvedOrganizationFromRequestContext().getSlug()
                        : null
        );
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
        log.debug("Content POST request: pathOrganizationId={}, moduleSlug={}, jwtOrganizationId={}, jwtOrganizationSlug={}, resolvedTenantSlug={}",
                organizationId,
                type,
                tenantAccessService.getCurrentJwtOrganizationId(),
                tenantAccessService.getCurrentJwtOrganizationSlug(),
                tenantAccessService.getResolvedOrganizationFromRequestContext() != null
                        ? tenantAccessService.getResolvedOrganizationFromRequestContext().getSlug()
                        : null
        );
        rbacService.requireTenantContentCreationAccess(organizationId);
        OrganizationContentType contentType = OrganizationContentType.fromPath(type);
        User actor = rbacService.getCurrentUserOrThrow();
        OrganizationContentDto created = contentService.createContent(organizationId, contentType, request, actor);
        log.debug("Content created: pathOrganizationId={}, moduleSlug={}, contentId={}, dtoOrganizationId={}, dtoType={}",
                organizationId, type, created.getId(), created.getOrganizationId(), created.getType());
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

    @PatchMapping("/{contentId}/published")
    public ResponseEntity<OrganizationContentDto> updatePublicationStatus(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("type") String type,
            @PathVariable("contentId") Long contentId,
            @RequestParam("published") Boolean published
    ) {
        rbacService.requireTenantContentCreationAccess(organizationId);
        OrganizationContentType contentType = OrganizationContentType.fromPath(type);
        return ResponseEntity.ok(
                contentService.updateContentPublicationStatus(organizationId, contentType, contentId, published)
        );
    }
}
