package museon_online.astor_butler.telegram;

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


    public void sendText(Long chatId, String text) {
        execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build());
    }

    public void sendHtml(Long chatId, String html, ReplyKeyboard kb) {
        execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text(html)
                .parseMode("HTML")
                .replyMarkup(kb)
                .build());
    }

    private void execute(BotApiMethod<?> method) {
        try {
            bot.execute(method);
        } catch (Exception e) {
            log.error("Telegram API call failed: {}", method.getMethod(), e);   // 🆕
        }
    }
}