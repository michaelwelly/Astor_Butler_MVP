package museon_online.astor_butler.domain.web;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import museon_online.astor_butler.telegram.adapter.TelegramAdminNotifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebLeadNotificationService {

    private final TelegramAdminNotifier telegramAdminNotifier;

    @Value("${astor.web.notifications.admin-chat-enabled:true}")
    private boolean adminChatEnabled;

    public void project(WebSessionResolution session, IncomingMessage incoming, OutgoingMessage outgoing) {
        if (!adminChatEnabled || session == null || incoming == null) {
            return;
        }
        String text = incoming.text() == null ? "" : incoming.text().trim();
        if (text.isBlank()) {
            return;
        }

        telegramAdminNotifier.sendAnalytics(card(session, incoming, outgoing));
    }

    private String card(WebSessionResolution session, IncomingMessage incoming, OutgoingMessage outgoing) {
        Map<String, Object> payload = incoming.payload() == null ? Map.of() : incoming.payload();
        return """
                <b>Astor Butler / website lead</b>
                Новое сообщение с сайта

                <b>Visitor</b>
                session %s
                chat %s / user %s

                <b>Message</b>
                <blockquote>%s</blockquote>

                <b>Context</b>
                Site: %s
                Page: %s
                Referrer: %s
                Selected video: %s
                Consent: %s

                <b>FSM</b>
                Next state: %s
                Fallback: %s
                Actions: %s

                <b>Correlation</b>
                %s
                """.formatted(
                html(session.sessionId()),
                html(text(session.chatId())),
                html(blank(session.externalUserId())),
                html(blank(incoming.text())),
                html(blank(value(payload, "site"))),
                html(blank(value(payload, "page"))),
                html(blank(value(payload, "referrer"))),
                html(blank(selectedVideo(payload))),
                html(blank(consent(payload))),
                html(outgoing == null ? "" : outgoing.nextState()),
                html(outgoing == null ? "" : text(outgoing.fallback())),
                html(outgoing == null || outgoing.actions() == null ? "" : String.join(", ", outgoing.actions())),
                html(blank(incoming.correlationId()))
        );
    }

    private String selectedVideo(Map<String, Object> payload) {
        Object selected = payload.get("selectedVideo");
        if (!(selected instanceof Map<?, ?> video)) {
            return "";
        }
        String slug = string(video.get("slug"));
        String title = string(video.get("title"));
        if (!slug.isBlank() && !title.isBlank()) {
            return title + " (" + slug + ")";
        }
        return !slug.isBlank() ? slug : title;
    }

    private String consent(Map<String, Object> payload) {
        Object consent = payload.get("consent");
        if (!(consent instanceof Map<?, ?> consentMap)) {
            return "";
        }
        Object accepted = consentMap.get("privacyAccepted");
        Object version = consentMap.get("policyVersion");
        return "privacyAccepted=%s, policyVersion=%s".formatted(
                string(accepted),
                string(version)
        );
    }

    private String value(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return string(value);
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "(empty)" : value;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private String html(String value) {
        return text(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
