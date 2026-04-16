package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.config.TenantContext;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.exception.TenantAccessDeniedException;
import tn.esprit.tic.civiAgora.exception.TenantResolutionException;

@Service
@RequiredArgsConstructor
public class TenantAccessService {

    private final OrganizationRepository organizationRepository;

    public Integer getCurrentJwtOrganizationId() {
        return TenantContext.getAuthenticatedOrganizationId();
    }

    public String getCurrentJwtOrganizationSlug() {
        return TenantContext.getAuthenticatedOrganizationSlug();
    }

    public Organization getResolvedOrganizationFromRequestContext() {
        Integer organizationId = TenantContext.getResolvedOrganizationId();
        if (organizationId != null) {
            return organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new TenantResolutionException("Resolved organization no longer exists"));
        }

        String organizationSlug = TenantContext.getResolvedOrganizationSlug();
        if (organizationSlug == null || organizationSlug.isBlank()) {
            return null;
        }

        return organizationRepository.findBySlug(organizationSlug)
                .orElseThrow(() -> new TenantResolutionException("Resolved organization no longer exists"));
    }

    public Organization getResolvedOrganizationOrThrow() {
        Organization organization = getResolvedOrganizationFromRequestContext();
        if (organization == null) {
            throw new TenantResolutionException("Tenant could not be resolved for this request");
        }
        return organization;
    }

    public void assertTenantMatchOrThrow() {
        Organization resolvedOrganization = getResolvedOrganizationOrThrow();
        if (isCurrentUserSuperAdmin()) {
            return;
        }

        Integer jwtOrganizationId = getCurrentJwtOrganizationId();
        String jwtOrganizationSlug = getCurrentJwtOrganizationSlug();

        if (jwtOrganizationId == null || jwtOrganizationSlug == null || jwtOrganizationSlug.isBlank()) {
            throw new TenantAccessDeniedException("Authenticated token does not carry tenant context");
        }

        if (!resolvedOrganization.getId().equals(jwtOrganizationId)) {
            throw new TenantAccessDeniedException("Authenticated tenant does not match the requested organization");
        }

        if (resolvedOrganization.getSlug() == null
                || !resolvedOrganization.getSlug().equalsIgnoreCase(jwtOrganizationSlug)) {
            throw new TenantAccessDeniedException("Authenticated tenant slug does not match the requested organization");
        }
    }

    public Organization getCurrentOrganizationEntityOrThrow() {
        assertTenantMatchOrThrow();
        return getResolvedOrganizationOrThrow();
    }

    public Organization assertOrganizationAccessOrThrow(Integer organizationId) {
        if (isCurrentUserSuperAdmin()) {
            return organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new TenantResolutionException("Requested organization does not exist"));
        }

        Organization currentOrganization = getCurrentOrganizationEntityOrThrow();
        if (!currentOrganization.getId().equals(organizationId)) {
            throw new TenantAccessDeniedException("Requested organization does not match the authenticated tenant");
        }
        return currentOrganization;
    }

    public boolean isCurrentUserSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        return user.getRole() == Role.SUPER_ADMIN;
    }
}
