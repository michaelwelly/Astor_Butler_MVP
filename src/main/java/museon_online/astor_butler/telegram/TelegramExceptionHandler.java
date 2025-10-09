package museon_online.astor_butler.telegram;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class TelegramExceptionHandler {

    public void handle(Update update, Exception e, AbsSender sender) {
        e.printStackTrace();

        Long chatId = extractChatId(update);
        if (chatId != null) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("⚠️ Произошла ошибка. Попробуйте позже.");

            try {
                sender.execute(message);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private Long extractChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }
}