package tn.esprit.tic.civiAgora.dto.chatDto;

import java.util.List;
import java.util.Map;

public record OllamaChatResult(String reply, List<ExecutedToolResult> toolResults) {
    public OllamaChatResult(String reply) {
        this(reply, List.of());
    }

    public record ExecutedToolResult(String toolName, Map<String, Object> arguments, String resultJson) {}
}
