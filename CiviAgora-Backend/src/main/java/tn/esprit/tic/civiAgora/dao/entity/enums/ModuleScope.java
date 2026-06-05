package tn.esprit.tic.civiAgora.dao.entity.enums;

public enum ModuleScope {
    FRONT_OFFICE,
    BACK_OFFICE,
    BOTH,
    SAAS_ONLY;

    public boolean allowsFrontOffice() {
        return this == FRONT_OFFICE || this == BOTH;
    }

    public boolean allowsBackOffice() {
        return this == BACK_OFFICE || this == BOTH;
    }

    public boolean allowsTenantAssignment() {
        return this != SAAS_ONLY;
    }

    public static ModuleScope resolveOrDefault(ModuleScope scope) {
        return scope == null ? BOTH : scope;
    }
}
