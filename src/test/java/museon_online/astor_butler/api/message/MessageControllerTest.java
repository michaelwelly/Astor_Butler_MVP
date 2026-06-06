package museon_online.astor_butler.api.message;

import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.MessageChannel;
import museon_online.astor_butler.service.message.MessageGatewayService;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageControllerTest {

    private final MessageGatewayService gatewayService = mock(MessageGatewayService.class);
    private final MessageController controller = new MessageController(gatewayService);

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

        controller.process(request);

        ArgumentCaptor<IncomingMessage> captor = ArgumentCaptor.forClass(IncomingMessage.class);
        verify(gatewayService).handle(captor.capture());
        IncomingMessage incoming = captor.getValue();

        assertThat(incoming.channel()).isEqualTo(MessageChannel.TELEGRAM);
        assertThat(incoming.externalUserId()).isEqualTo("900001001");
        assertThat(incoming.telegramUserId()).isEqualTo(900001001L);
        assertThat(incoming.chatId()).isEqualTo(900001001L);
    }
}
