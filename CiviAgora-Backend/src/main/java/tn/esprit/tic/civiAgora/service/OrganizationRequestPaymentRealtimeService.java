package tn.esprit.tic.civiAgora.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestPaymentEventDto;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class OrganizationRequestPaymentRealtimeService {

    private static final String EVENT_NAME = "organization-request-payment";
    private static final long SSE_TIMEOUT_MILLIS = 0L;

    private final List<SseEmitter> saasSubscribers = new CopyOnWriteArrayList<>();
    private final Map<String, List<SseEmitter>> tokenSubscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribeToBackOffice() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        registerEmitter(emitter, saasSubscribers, null);
        return emitter;
    }

    public SseEmitter subscribeToPaymentToken(String paymentToken) {
        if (paymentToken == null || paymentToken.isBlank()) {
            throw new IllegalArgumentException("Payment token is required");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        registerEmitter(emitter, tokenSubscribers.computeIfAbsent(paymentToken, key -> new CopyOnWriteArrayList<>()), paymentToken);
        return emitter;
    }

    public void publish(OrganizationRequestPaymentEventDto payload) {
        if (payload == null) {
            return;
        }

        sendEvent(saasSubscribers, payload);

        if (payload.getPaymentToken() != null && !payload.getPaymentToken().isBlank()) {
            List<SseEmitter> tokenEmitters = tokenSubscribers.get(payload.getPaymentToken());
            if (tokenEmitters != null) {
                sendEvent(tokenEmitters, payload);
            }
        }
    }

    private void registerEmitter(SseEmitter emitter, List<SseEmitter> subscribers, String token) {
        subscribers.add(emitter);

        emitter.onCompletion(() -> removeEmitter(subscribers, emitter, token));
        emitter.onTimeout(() -> completeAndRemove(subscribers, emitter, token));
        emitter.onError((error) -> completeAndRemove(subscribers, emitter, token));
    }

    private void sendEvent(List<SseEmitter> subscribers, OrganizationRequestPaymentEventDto payload) {
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : List.copyOf(subscribers)) {
            send(emitter, payload);
        }
    }

    private void send(SseEmitter emitter, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME)
                    .data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
    }

    private void removeEmitter(List<SseEmitter> subscribers, SseEmitter emitter, String token) {
        subscribers.remove(emitter);
        if (token != null && tokenSubscribers.containsKey(token) && subscribers.isEmpty()) {
            tokenSubscribers.remove(token);
        }
    }

    private void completeAndRemove(List<SseEmitter> subscribers, SseEmitter emitter, String token) {
        try {
            emitter.complete();
        } finally {
            removeEmitter(subscribers, emitter, token);
        }
    }
}
