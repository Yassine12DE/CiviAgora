package tn.esprit.tic.civiAgora.config;

public class TenantContext {
    private static final ThreadLocal<Integer> resolvedOrganizationId = new ThreadLocal<>();
    private static final ThreadLocal<String> resolvedOrganizationSlug = new ThreadLocal<>();
    private static final ThreadLocal<Integer> authenticatedOrganizationId = new ThreadLocal<>();
    private static final ThreadLocal<String> authenticatedOrganizationSlug = new ThreadLocal<>();

    public static Integer getResolvedOrganizationId() {
        return resolvedOrganizationId.get();
    }

    public static String getResolvedOrganizationSlug() {
        return resolvedOrganizationSlug.get();
    }

    public static void setResolvedOrganization(Integer organizationId, String organizationSlug) {
        if (organizationId == null || organizationSlug == null || organizationSlug.isBlank()) {
            clearResolvedTenant();
            return;
        }

        resolvedOrganizationId.set(organizationId);
        resolvedOrganizationSlug.set(organizationSlug);
    }

    public static Integer getAuthenticatedOrganizationId() {
        return authenticatedOrganizationId.get();
    }

    public static String getAuthenticatedOrganizationSlug() {
        return authenticatedOrganizationSlug.get();
    }

    public static void setAuthenticatedOrganization(Integer organizationId, String organizationSlug) {
        if (organizationId == null || organizationSlug == null || organizationSlug.isBlank()) {
            clearAuthenticatedTenant();
            return;
        }

        authenticatedOrganizationId.set(organizationId);
        authenticatedOrganizationSlug.set(organizationSlug);
    }

    public static String getCurrentOrganizationSlug() {
        return getResolvedOrganizationSlug();
    }

    public static void setCurrentOrganizationSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            clearResolvedTenant();
            return;
        }

        resolvedOrganizationId.remove();
        resolvedOrganizationSlug.set(slug);
    }

    public static void clearResolvedTenant() {
        resolvedOrganizationId.remove();
        resolvedOrganizationSlug.remove();
    }

    public static void clearAuthenticatedTenant() {
        authenticatedOrganizationId.remove();
        authenticatedOrganizationSlug.remove();
    }

    public static void clear() {
        clearResolvedTenant();
        clearAuthenticatedTenant();
    }
}
