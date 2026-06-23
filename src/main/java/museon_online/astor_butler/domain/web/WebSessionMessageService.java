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
        String payloadSessionId = string(safePayload, "sessionId");
        String externalSessionId = stripWebAnonPrefix(externalUserId);
        String sessionId = firstNonBlank(
                payloadSessionId,
                externalSessionId,
                UUID.randomUUID().toString()
        );
        Long compatibilityChatId = hasStableSession(payloadSessionId, externalSessionId) ? null : requestedChatId;
        WebSessionResolution session = repository.upsert(
                stringOrDefault(safePayload, "site", "c3flex"),
                sessionId,
                externalUserId,
                compatibilityChatId,
                string(safePayload, "referrer"),
                string(safePayload, "page"),
                string(safePayload, "userAgentHash"),
                safePayload
        );
        repository.upsertConsentIfPresent(session, safePayload);
        return session;
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

    public WebSessionResolution grantAnonymousConsent(String source, String policyVersion, Map<String, Object> evidence) {
        Map<String, Object> safeEvidence = evidence == null ? Map.of() : evidence;
        Map<String, Object> consent = new LinkedHashMap<>();
        consent.put("privacyAccepted", true);
        consent.put("policyVersion", firstNonBlank(policyVersion, string(safeEvidence, "policyVersion"), "2026-06-02-local"));
        consent.put("acceptedAt", string(safeEvidence, "acceptedAt"));
        consent.put("source", source == null || source.isBlank() ? "WEB" : source);

        Map<String, Object> payload = new LinkedHashMap<>(safeEvidence);
        payload.put("site", stringOrDefault(payload, "site", "c3flex"));
        payload.put("consent", consent);
        return resolve(null, null, payload);
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

    private boolean hasStableSession(String payloadSessionId, String externalSessionId) {
        return (payloadSessionId != null && !payloadSessionId.isBlank())
                || (externalSessionId != null && !externalSessionId.isBlank());
    }

    private String string(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? null : value.toString();
    }
}
