package tn.esprit.tic.civiAgora.tools;

import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.User;

/**
 * Trusted context created for every tool invocation. Values in this record never
 * come from model arguments, so a tool cannot switch users or tenants.
 */
public record ToolExecutionContext(User user, Organization organization, String currentUserMessage) {
    public Integer userId() {
        return user.getId();
    }

    public Integer organizationId() {
        return organization.getId();
    }

    public String organizationSlug() {
        return organization.getSlug();
    }
}
