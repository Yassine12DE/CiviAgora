package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final TenantAccessService tenantAccessService;

    public User getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AccessDeniedException("Authenticated user required");
        }
        return user;
    }

    public void requireTenantBackOfficeAccess(Integer organizationId) {
        requireTenantRole(organizationId, Role.ADMIN, Role.MANAGER, Role.MODERATOR);
    }

    public void requireTenantModuleVisibilityAccess(Integer organizationId) {
        requireTenantRole(organizationId, Role.ADMIN);
    }

    public void requireTenantUserManagementAccess(Integer organizationId) {
        requireTenantRole(organizationId, Role.ADMIN, Role.MANAGER);
    }

    public void requireTenantDesignCustomizationAccess(Integer organizationId) {
        requireTenantRole(organizationId, Role.ADMIN, Role.MODERATOR);
    }

    public void requireTenantModuleRequestAccess(Integer organizationId) {
        requireTenantRole(organizationId, Role.ADMIN, Role.MODERATOR);
    }

    public void requireTenantContentCreationAccess(Integer organizationId) {
        requireTenantRole(organizationId, Role.ADMIN, Role.MANAGER);
    }

    public void requireTenantContentAccess(Integer organizationId) {
        requireTenantRole(organizationId, Role.ADMIN, Role.MANAGER, Role.MODERATOR, Role.CITIZEN);
    }

    public void requireTenantContentInteractionAccess(Integer organizationId) {
        requireTenantRole(organizationId, Role.ADMIN, Role.MANAGER, Role.MODERATOR, Role.CITIZEN);
    }

    public void requireTenantRole(Integer organizationId, Role... roles) {
        tenantAccessService.assertOrganizationAccessOrThrow(organizationId);

        User user = getCurrentUserOrThrow();
        if (user.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        Set<Role> allowedRoles = Arrays.stream(roles).collect(Collectors.toSet());
        if (!allowedRoles.contains(user.getRole())) {
            throw new AccessDeniedException("This role is not allowed to access this tenant feature");
        }
    }

    public boolean isManagerLimitedToCitizenUsers() {
        return getCurrentUserOrThrow().getRole() == Role.MANAGER;
    }
}
