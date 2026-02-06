package museon_online.astor_butler.telegram.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;

/**
 * Утилита-курьер для отправки сообщений.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramSender {

    private final AbsSender bot;

    /**
     * Отправка простого текстового сообщения без клавиатуры.
     */
    public void sendText(Long chatId, String text) {
        execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build());
    }

    /**
     * Отправка текстового сообщения с клавиатурой.
     */
    public void sendText(Long chatId, String text, ReplyKeyboard keyboard) {
        execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard)
                .build());
    }

    /**
     * Отправка HTML-сообщения с клавиатурой.
     */
    public void sendHtml(Long chatId, String html, ReplyKeyboard keyboard) {
        execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text(html)
                .parseMode("HTML")
                .replyMarkup(keyboard)
                .build());
    }

    /**
     * Безопасное выполнение API-вызова Telegram.
     */
    private void execute(BotApiMethod<?> method) {
        try {
            bot.execute(method);
        } catch (Exception e) {
            log.error("Telegram API call failed: {}", method.getMethod(), e);
        }
    }
}