package museon_online.astor_butler.telegram.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import museon_online.astor_butler.telegram.utils.TelegramBot;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramSystemNotifier {

    private final ObjectProvider<TelegramBot> telegramBotProvider;

    @Value("${telegram.bot.enabled:false}")
    private boolean telegramEnabled;

    @Value("${telegram.system.chat-id:}")
    private String systemChatId;

    @Value("${telegram.system.notifications-enabled:false}")
    private boolean notificationsEnabled;

    public void sendTransition(IncomingMessage incoming, BotState previousState, OutgoingMessage outgoing) {
        if (!telegramEnabled || !notificationsEnabled || systemChatId == null || systemChatId.isBlank()) {
            return;
        }

        TelegramBot telegramBot = telegramBotProvider.getIfAvailable();
        if (telegramBot == null) {
            return;
        }

        try {
            telegramBot.execute(SendMessage.builder()
                    .chatId(systemChatId)
                    .text(systemText(incoming, previousState, outgoing))
                    .parseMode("HTML")
                    .build());
        } catch (Exception e) {
            log.warn("Telegram system notification failed: {}", e.getMessage());
        }
    }

    private String systemText(IncomingMessage incoming, BotState previousState, OutgoingMessage outgoing) {
        return """
                <b>Astor Butler / system</b>
                FSM transition

                <b>Guest</b>
                chat %s / user %s%s

                <b>State</b>
                %s -> %s

                <b>Actions</b>
                %s

                <b>Text</b>
                <blockquote>%s</blockquote>

                <b>Correlation</b>
                %s
                """.formatted(
                html(text(incoming == null ? null : incoming.chatId())),
                html(text(incoming == null ? null : incoming.telegramUserId())),
                incoming == null || incoming.username() == null || incoming.username().isBlank()
                        ? ""
                        : " / @" + html(incoming.username()),
                html(text(previousState)),
                html(outgoing == null ? "" : outgoing.nextState()),
                html(outgoing == null || outgoing.actions() == null ? "" : String.join(", ", outgoing.actions())),
                html(blank(incoming == null ? null : incoming.text())),
                html(blank(incoming == null ? null : incoming.correlationId()))
        );
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "(empty)" : value;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String html(String value) {
        return text(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
