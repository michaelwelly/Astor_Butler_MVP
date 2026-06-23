package museon_online.astor_butler.domain.web;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSessionMessageService {

    private final WebSessionRepository repository;

    public WebSessionResolution resolve(String externalUserId, Long requestedChatId, Map<String, Object> payload) {
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        String sessionId = firstNonBlank(
                string(safePayload, "sessionId"),
                stripWebAnonPrefix(externalUserId),
                UUID.randomUUID().toString()
        );
        return repository.upsert(
                stringOrDefault(safePayload, "site", "c3flex"),
                sessionId,
                externalUserId,
                requestedChatId,
                string(safePayload, "referrer"),
                string(safePayload, "page"),
                string(safePayload, "userAgentHash"),
                safePayload
        );
    }

    public void recordInbound(WebSessionResolution session, String correlationId, String text, Map<String, Object> payload) {
        repository.appendMessage(session, correlationId, "IN", text, payload == null ? Map.of() : payload);
    }

    public void recordOutbound(WebSessionResolution session, String correlationId, OutgoingMessage outgoing) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nextState", outgoing.nextState());
        payload.put("fallback", outgoing.fallback());
        payload.put("adminAlertRequired", outgoing.adminAlert() != null && outgoing.adminAlert().required());
        payload.put("actions", outgoing.actions() == null ? java.util.List.of() : outgoing.actions());
        payload.put("metadata", outgoing.metadata() == null ? Map.of() : outgoing.metadata());
        repository.appendMessage(session, correlationId, "OUT", outgoing.text(), payload);
    }

    private String stripWebAnonPrefix(String externalUserId) {
        if (externalUserId == null || externalUserId.isBlank()) {
            return null;
        }
        String trimmed = externalUserId.trim();
        return trimmed.startsWith("web:anon:") ? trimmed.substring("web:anon:".length()) : null;
    }

    private String stringOrDefault(Map<String, Object> payload, String key, String defaultValue) {
        String value = string(payload, key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return UUID.randomUUID().toString();
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return UUID.randomUUID().toString();
    }

    private String string(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? null : value.toString();
    }
}
