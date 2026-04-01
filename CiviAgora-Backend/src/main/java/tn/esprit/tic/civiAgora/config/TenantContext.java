package tn.esprit.tic.civiAgora.config;

public class TenantContext {
    private static final ThreadLocal<String> currentOrganizationSlug = new ThreadLocal<>();

    public static String getCurrentOrganizationSlug() {
        return currentOrganizationSlug.get();
    }

    public static void setCurrentOrganizationSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            currentOrganizationSlug.remove();
        } else {
            currentOrganizationSlug.set(slug);
        }
    }

    public static void clear() {
        currentOrganizationSlug.remove();
    }
}
