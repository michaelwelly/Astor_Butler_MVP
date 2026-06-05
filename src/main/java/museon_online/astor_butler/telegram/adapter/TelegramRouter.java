package museon_online.astor_butler.telegram.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.event.InboundEvent;
import museon_online.astor_butler.fsm.core.idempotency.IdempotencyGuard;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.MessageGatewayService;
import museon_online.astor_butler.service.message.OutgoingMessage;
import museon_online.astor_butler.telegram.exeption.TelegramExceptionHandler;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.ResponseParameters;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramRouter {

    private final TelegramExceptionHandler exceptionHandler;
    private final IdempotencyGuard idempotencyGuard;
    private final MessageGatewayService messageGatewayService;


    public void handle(Update update, AbsSender sender) {
        try {
            if (!acceptInboundEvent(update)) {
                return;
            }

            IncomingMessage incoming = toIncomingMessage(update);
            if (incoming == null) {
                log.debug("📭 [TG] Update ignored: cannot map to IncomingMessage");
                return;
            }

            log.info("📨 [TG] Incoming message from {}: {}", incoming.chatId(), incoming.text());

            OutgoingMessage outgoing = messageGatewayService.handle(incoming);
            send(outgoing, sender);
            sendAdminAlert(outgoing, sender);

        } catch (Exception e) {
            log.error("💥 [TG] Exception while handling update: {}", e.getMessage(), e);
            exceptionHandler.handle(update, e, sender);
        }
    }
    private boolean acceptInboundEvent(Update update) {
        try {
            InboundEvent inboundEvent = InboundEvent.from(update);

            if (inboundEvent == null) {
                log.debug("📭 [PIPELINE] Update ignored (cannot be mapped to InboundEvent)");
                return true;
            }

            boolean accepted = idempotencyGuard.accept(inboundEvent);
            if (!accepted) {
                log.info(
                        "🔁 [PIPELINE] Duplicate event ignored (eventId={}, chatId={})",
                        inboundEvent.getEventId(),
                        inboundEvent.getChatId()
                );
                return false;
            }

            log.info(
                    "➡️ [PIPELINE] InboundEvent accepted by idempotency guard → forwarding to MessageGateway (eventId={}, type={}, chatId={})",
                    inboundEvent.getEventId(),
                    inboundEvent.getType(),
                    inboundEvent.getChatId()
            );

            return true;

        } catch (Exception e) {
            log.error("💥 [PIPELINE] Error while processing inbound event", e);
            return true;
        }
    }

    private IncomingMessage toIncomingMessage(Update update) {
        if (update == null || update.getMessage() == null) {
            return null;
        }

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        User user = message.getFrom();
        Contact contact = message.getContact();

        return IncomingMessage.telegram(
                chatId,
                user == null ? null : user.getId(),
                message.getMessageId(),
                update.getUpdateId(),
                message.hasText() ? message.getText() : "",
                contact == null ? null : contact.getPhoneNumber(),
                user == null ? null : user.getFirstName(),
                user == null ? null : user.getLastName(),
                user == null ? null : user.getUserName(),
                user == null ? null : user.getLanguageCode(),
                user == null ? null : user.getIsBot(),
                update.getUpdateId() == null ? UUID.randomUUID().toString() : update.getUpdateId().toString()
        );
    }

    private void send(OutgoingMessage outgoing, AbsSender sender) {
        if (outgoing == null || outgoing.chatId() == null || outgoing.text() == null || outgoing.text().isBlank()) {
            return;
        }

        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(outgoing.chatId().toString())
                .text(outgoing.text());

        if (outgoing.html()) {
            builder.parseMode("HTML");
        }
        if (outgoing.requestContact()) {
            builder.replyMarkup(contactKeyboard());
        } else if (outgoing.removeKeyboard()) {
            builder.replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
        }

        execute(sender, builder.build());
    }

    private void sendAdminAlert(OutgoingMessage outgoing, AbsSender sender) {
        if (outgoing == null
                || outgoing.adminAlert() == null
                || !outgoing.adminAlert().required()
                || outgoing.adminAlert().chatId() == null
                || outgoing.adminAlert().chatId().isBlank()) {
            return;
        }

        execute(sender, SendMessage.builder()
                .chatId(outgoing.adminAlert().chatId())
                .text(outgoing.adminAlert().text())
                .parseMode("HTML")
                .build());
    }

    private ReplyKeyboardMarkup contactKeyboard() {
        KeyboardButton shareContact = KeyboardButton.builder()
                .text("Согласиться и поделиться контактом")
                .requestContact(true)
                .build();

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(new KeyboardRow(List.of(shareContact))))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }

    private void execute(AbsSender sender, BotApiMethod<?> method) {
        try {
            sender.execute(method);
        } catch (TelegramApiRequestException e) {
            Long migratedChatId = migratedChatId(e);
            if (migratedChatId != null && method instanceof SendMessage sendMessage) {
                log.warn(
                        "Telegram chat migrated to supergroup. Update TELEGRAM_ADMIN_CHAT_ID/TELEGRAM_ANALYTICS_CHAT_ID to {}. Retrying once.",
                        migratedChatId
                );
                try {
                    sendMessage.setChatId(migratedChatId);
                    sender.execute(sendMessage);
                    return;
                } catch (Exception retryException) {
                    log.error("Telegram API retry after chat migration failed: {}", method.getMethod(), retryException);
                    return;
                }
            }
            log.error("Telegram API call failed: {}", method.getMethod(), e);
        } catch (Exception e) {
            log.error("Telegram API call failed: {}", method.getMethod(), e);
        }
    }

    private Long migratedChatId(TelegramApiRequestException e) {
        ResponseParameters parameters = e.getParameters();
        if (parameters != null && parameters.getMigrateToChatId() != null) {
            return parameters.getMigrateToChatId();
        }

        String apiResponse = e.getApiResponse();
        if (apiResponse == null || apiResponse.isBlank()) {
            return null;
        }

        int marker = apiResponse.indexOf("migrate_to_chat_id");
        if (marker < 0) {
            return null;
        }

        String suffix = apiResponse.substring(marker).replaceAll("[^0-9-]", " ").trim();
        if (suffix.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(suffix.split("\\s+")[0]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
