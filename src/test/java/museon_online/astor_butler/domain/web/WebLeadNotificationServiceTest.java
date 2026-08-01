package museon_online.astor_butler.domain.web;

import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.MessageChannel;
import museon_online.astor_butler.service.message.OutgoingMessage;
import museon_online.astor_butler.telegram.adapter.TelegramAdminNotifier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class WebLeadNotificationServiceTest {

    private final TelegramAdminNotifier telegramAdminNotifier = mock(TelegramAdminNotifier.class);
    private final WebLeadNotificationService service = new WebLeadNotificationService(telegramAdminNotifier);

    @Test
    void projectsWebsiteMessageToTelegramOperatorCard() {
        ReflectionTestUtils.setField(service, "adminChatEnabled", true);
        WebSessionResolution session = new WebSessionResolution(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "session-42",
                "web:anon:session-42",
                900000042L
        );
        IncomingMessage incoming = new IncomingMessage(
                MessageChannel.WEB,
                session.externalUserId(),
                session.chatId(),
                null,
                null,
                null,
                "Хочу фильм о заводе <важно>",
                null,
                null,
                null,
                null,
                null,
                false,
                "corr-42",
                Instant.parse("2026-08-01T06:00:00Z"),
                Map.of(
                        "site", "c3ag",
                        "page", "/film",
                        "referrer", "https://example.com/",
                        "selectedVideo", Map.of("slug", "umekon-zavod", "title", "Сериал ЗАВОД"),
                        "consent", Map.of("privacyAccepted", true, "policyVersion", "2026-06-02-local")
                )
        );
        OutgoingMessage outgoing = OutgoingMessage.of(
                incoming,
                "Принял",
                "WEB_LEAD_RECEIVED",
                false,
                false,
                false,
                false,
                null,
                List.of("WEB_LEAD_CAPTURED", "ADMIN_ALERT")
        );

        service.project(session, incoming, outgoing);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramAdminNotifier).sendAnalytics(captor.capture());
        String card = captor.getValue();
        assertThat(card).contains("website lead");
        assertThat(card).contains("lead 11111111-1111-1111-1111-111111111111");
        assertThat(card).contains("message corr-42");
        assertThat(card).contains("time 2026-08-01T06:00:00Z");
        assertThat(card).contains("session session-42");
        assertThat(card).contains("chat 900000042");
        assertThat(card).contains("Хочу фильм о заводе &lt;важно&gt;");
        assertThat(card).contains("Selected video: Сериал ЗАВОД (umekon-zavod)");
        assertThat(card).contains("Next state: WEB_LEAD_RECEIVED");
        assertThat(card).contains("WEB_LEAD_CAPTURED, ADMIN_ALERT");
    }

    @Test
    void skipsBlankMessages() {
        ReflectionTestUtils.setField(service, "adminChatEnabled", true);
        WebSessionResolution session = new WebSessionResolution(UUID.randomUUID(), "session", "web:anon:session", 1L);
        IncomingMessage incoming = new IncomingMessage(
                MessageChannel.WEB,
                session.externalUserId(),
                session.chatId(),
                null,
                null,
                null,
                "  ",
                null,
                null,
                null,
                null,
                null,
                false,
                "corr",
                Instant.now(),
                Map.of()
        );

        service.project(session, incoming, null);

        verifyNoInteractions(telegramAdminNotifier);
    }
}
