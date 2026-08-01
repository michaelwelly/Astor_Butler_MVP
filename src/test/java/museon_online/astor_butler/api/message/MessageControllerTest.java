package museon_online.astor_butler.api.message;

import museon_online.astor_butler.domain.web.WebLeadNotificationService;
import museon_online.astor_butler.domain.web.WebChatRateLimiter;
import museon_online.astor_butler.domain.web.WebSessionMessageService;
import museon_online.astor_butler.domain.web.WebSessionResolution;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.MessageChannel;
import museon_online.astor_butler.service.message.MessageGatewayService;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageControllerTest {

    private final MessageGatewayService gatewayService = mock(MessageGatewayService.class);
    private final WebSessionMessageService webSessionMessageService = mock(WebSessionMessageService.class);
    private final WebLeadNotificationService webLeadNotificationService = mock(WebLeadNotificationService.class);
    private final WebChatRateLimiter webChatRateLimiter = mock(WebChatRateLimiter.class);
    private final MessageController controller = new MessageController(
            gatewayService,
            webSessionMessageService,
            webLeadNotificationService,
            webChatRateLimiter
    );

    @Test
    void mapsTelegramExternalUserIdToTelegramUserIdForFsmSimulation() {
        MessageController.MessageRequest request = new MessageController.MessageRequest(
                "TELEGRAM",
                "900001001",
                900001001L,
                "Привет",
                null,
                "Анна",
                "weekend_anna",
                "test-correlation",
                Map.of()
        );

        when(gatewayService.handle(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> OutgoingMessage.of(
                        invocation.getArgument(0),
                        "ok",
                        "CONSENT_REQUIRED",
                        false,
                        true,
                        false,
                        false,
                        null,
                        List.of("REQUEST_CONTACT")
                ));
        when(webChatRateLimiter.check(any(), any(), any(), any()))
                .thenReturn(WebChatRateLimiter.Decision.allow());

        controller.process(request);

        ArgumentCaptor<IncomingMessage> captor = ArgumentCaptor.forClass(IncomingMessage.class);
        verify(gatewayService).handle(captor.capture());
        IncomingMessage incoming = captor.getValue();

        assertThat(incoming.channel()).isEqualTo(MessageChannel.TELEGRAM);
        assertThat(incoming.externalUserId()).isEqualTo("900001001");
        assertThat(incoming.telegramUserId()).isEqualTo(900001001L);
        assertThat(incoming.chatId()).isEqualTo(900001001L);
    }

    @Test
    void webMessageUsesFastPathAndProjectsTelegramOperatorNotification() {
        Map<String, Object> payload = Map.of(
                "site", "c3ag",
                "sessionId", "web-session-1",
                "page", "/film",
                "selectedVideo", Map.of("slug", "umekon-zavod", "title", "Сериал ЗАВОД")
        );
        WebSessionResolution session = new WebSessionResolution(
                UUID.randomUUID(),
                "web-session-1",
                "web:anon:web-session-1",
                900000123L
        );
        MessageController.MessageRequest request = new MessageController.MessageRequest(
                "WEB",
                "web:anon:web-session-1",
                null,
                "Хочу фильм о заводе",
                null,
                null,
                null,
                "web-correlation-1",
                payload
        );

        when(webSessionMessageService.resolve(eq("web:anon:web-session-1"), eq(null), eq(payload)))
                .thenReturn(session);
        when(webChatRateLimiter.check(any(), any(), any(), any()))
                .thenReturn(WebChatRateLimiter.Decision.allow());

        MessageController.MessageResponse response = controller.process(request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.channel()).isEqualTo("WEB");
        assertThat(response.chatId()).isEqualTo(900000123L);
        assertThat(response.nextState()).isEqualTo("WEB_LEAD_RECEIVED");
        assertThat(response.actions()).contains("WEB_LEAD_CAPTURED", "ADMIN_ALERT");

        verify(webSessionMessageService).recordInbound(session, "web-correlation-1", "Хочу фильм о заводе", payload);
        verify(webSessionMessageService).recordOutbound(eq(session), eq("web-correlation-1"), any(OutgoingMessage.class));
        verify(webLeadNotificationService).project(eq(session), any(IncomingMessage.class), any(OutgoingMessage.class));
        verify(gatewayService, never()).handle(any());
    }
}
