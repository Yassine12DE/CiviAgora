package tn.esprit.tic.civiAgora.dao.entity.enums;

public enum OrganizationContentType {
    VOTE("VOTE"),
    CONCERTATION("CONFERENCE"),
    YOUTH_NEWS("YOUTHSPACE");

    private final String moduleCode;

    OrganizationContentType(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public static OrganizationContentType fromPath(String rawType) {
        if (rawType == null) {
            throw new IllegalArgumentException("Content type is required");
        }

        String normalized = rawType.trim().toUpperCase().replace("-", "_");
        return switch (normalized) {
            case "VOTE", "VOTES" -> VOTE;
            case "CONCERTATION", "CONCERTATIONS", "CONFERENCE" -> CONCERTATION;
            case "YOUTH_NEWS", "YOUTH_NEWS_ITEM", "YOUTHSPACE", "NEWS" -> YOUTH_NEWS;
            default -> throw new IllegalArgumentException("Unsupported content type: " + rawType);
        };
    }
}
