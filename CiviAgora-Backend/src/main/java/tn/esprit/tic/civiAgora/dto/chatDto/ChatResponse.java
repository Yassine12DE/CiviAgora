package tn.esprit.tic.civiAgora.dto.chatDto;

public record ChatResponse(
        String reply,
        boolean authenticated,
        String organizationSlug,
        ChatUiPayload uiPayload
) {
    public ChatResponse(String reply, boolean authenticated, String organizationSlug) {
        this(reply, authenticated, organizationSlug, null);
    }
}
