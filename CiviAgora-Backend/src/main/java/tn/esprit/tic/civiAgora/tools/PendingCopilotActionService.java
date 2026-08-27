package tn.esprit.tic.civiAgora.tools;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PendingCopilotActionService {
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Set<String> CONFIRMATIONS = Set.of(
            "confirm", "confirmed", "i confirm", "yes confirm", "yes, confirm",
            "confirm the action", "je confirme", "oui je confirme", "oui, je confirme",
            "confirmer", "تأكيد", "أؤكد"
    );

    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();

    public PendingAction prepare(ToolExecutionContext context, String type,
                                 Map<String, Object> parameters, String summary) {
        PendingAction action = new PendingAction(
                type,
                Map.copyOf(new LinkedHashMap<>(parameters)),
                summary,
                Instant.now().plus(TTL)
        );
        pendingActions.put(key(context), action);
        return action;
    }

    public PendingAction requireConfirmed(ToolExecutionContext context) {
        if (!isExplicitConfirmation(context.currentUserMessage())) {
            throw new ConfirmationRequiredException("Reply exactly 'Confirm' to execute the pending action.");
        }
        PendingAction action = current(context);
        if (action == null) {
            throw new ConfirmationRequiredException("There is no pending action to confirm.");
        }
        return action;
    }

    public PendingAction current(ToolExecutionContext context) {
        PendingAction action = pendingActions.get(key(context));
        if (action != null && action.expiresAt().isBefore(Instant.now())) {
            pendingActions.remove(key(context), action);
            return null;
        }
        return action;
    }

    public void clear(ToolExecutionContext context) {
        pendingActions.remove(key(context));
    }

    public boolean isExplicitConfirmation(String message) {
        if (message == null) return false;
        String normalized = message.trim().toLowerCase(Locale.ROOT).replaceAll("[.!]+$", "");
        return CONFIRMATIONS.contains(normalized);
    }

    private String key(ToolExecutionContext context) {
        return context.organizationId() + ":" + context.userId();
    }

    public record PendingAction(String type, Map<String, Object> parameters,
                                String summary, Instant expiresAt) {
    }

    public static class ConfirmationRequiredException extends RuntimeException {
        public ConfirmationRequiredException(String message) {
            super(message);
        }
    }
}
