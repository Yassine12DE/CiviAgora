package tn.esprit.tic.civiAgora.dto.chatDto;

import java.util.List;

public record ChatRequest(
        String message,
        List<MessageEntry> history
) {
    public record MessageEntry(String role, String content) {}
}
