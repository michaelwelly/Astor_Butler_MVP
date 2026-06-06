package museon_online.astor_butler.api.message;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.api.common.ErrorCode;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.MessageChannel;
import museon_online.astor_butler.service.message.MessageGatewayService;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "FSM API", description = "FSM events, message gateway and safe state transitions")
public class MessageController {

    private final MessageGatewayService messageGatewayService;

    public MessageController(MessageGatewayService messageGatewayService) {
        this.messageGatewayService = messageGatewayService;
    }

    @PostMapping
    @Operation(
            summary = "Process normalized UI message through FSM gateway",
            description = "Common entry point for future web chat, Telegram-like adapters and smoke checks. Telegram stays a UI transport; FSM remains the source of truth."
    )
    public ResponseEntity<MessageResponse> process(@RequestBody MessageRequest request) {
        if (request.chatId() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "chatId is required for current MVP message gateway"
            );
        }

        MessageChannel messageChannel = channel(request.channel());
        String correlationId = request.correlationId() == null ? UUID.randomUUID().toString() : request.correlationId();
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();

        IncomingMessage incoming = messageChannel == MessageChannel.TELEGRAM
                ? IncomingMessage.telegram(
                        request.chatId(),
                        telegramUserId(request.externalUserId(), request.chatId()),
                        null,
                        null,
                        request.text(),
                        request.contactPhone(),
                        request.firstName(),
                        null,
                        request.username(),
                        null,
                        false,
                        correlationId,
                        payload
                )
                : new IncomingMessage(
                        messageChannel,
                        request.externalUserId(),
                        request.chatId(),
                        null,
                        null,
                        null,
                        request.text(),
                        request.contactPhone(),
                        request.firstName(),
                        null,
                        request.username(),
                        null,
                        false,
                        correlationId,
                        Instant.now(),
                        payload
                );

        return ResponseEntity.ok(MessageResponse.from(messageGatewayService.handle(incoming)));
    }

    private MessageChannel channel(String channel) {
        if (channel == null || channel.isBlank()) {
            return MessageChannel.WEB;
        }
        try {
            return MessageChannel.valueOf(channel.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Unsupported message channel",
                    Map.of("channel", channel)
            );
        }
    }

    private Long telegramUserId(String externalUserId, Long chatId) {
        if (externalUserId != null && !externalUserId.isBlank()) {
            try {
                return Long.parseLong(externalUserId.trim());
            } catch (NumberFormatException e) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST,
                        "Telegram externalUserId must be numeric",
                        Map.of("externalUserId", externalUserId)
                );
            }
        }
        return chatId != null && chatId > 0 ? chatId : null;
    }

    public record MessageRequest(
            String channel,
            String externalUserId,
            Long chatId,
            String text,
            String contactPhone,
            String firstName,
            String username,
            String correlationId,
            Map<String, Object> payload
    ) {
    }

    public record MessageResponse(
            String channel,
            String externalUserId,
            Long chatId,
            String text,
            String nextState,
            boolean html,
            boolean requestContact,
            boolean removeKeyboard,
            boolean fallback,
            boolean adminAlertRequired,
            List<String> actions,
            Map<String, Object> metadata,
            Instant createdAt
    ) {
        static MessageResponse from(OutgoingMessage outgoing) {
            return new MessageResponse(
                    outgoing.channel().name(),
                    outgoing.externalUserId(),
                    outgoing.chatId(),
                    outgoing.text(),
                    outgoing.nextState(),
                    outgoing.html(),
                    outgoing.requestContact(),
                    outgoing.removeKeyboard(),
                    outgoing.fallback(),
                    outgoing.adminAlert() != null && outgoing.adminAlert().required(),
                    outgoing.actions(),
                    outgoing.metadata(),
                    outgoing.createdAt()
            );
        }
    }
}
