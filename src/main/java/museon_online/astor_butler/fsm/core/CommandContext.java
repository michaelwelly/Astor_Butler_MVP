package museon_online.astor_butler.fsm.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import museon_online.astor_butler.fsm.core.event.InboundEvent;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Data
@AllArgsConstructor
public class CommandContext {
    private final Long chatId;
    private final String messageText;
    private final Contact contact;
    private final Message message;
    private final Update update;
    private final User telegramUser;

    // 🔹 Вспомогательный конструктор для универсальной сборки контекста
    public static CommandContext from(Update update) {
        if (update == null) {
            return new CommandContext(null, "", null, null, null, null);
        }

        if (update.hasCallbackQuery()) {
            Message callbackMessage = update.getCallbackQuery().getMessage();
            return new CommandContext(
                    callbackMessage != null ? callbackMessage.getChatId() : null,
                    update.getCallbackQuery().getData(),
                    null,
                    callbackMessage,
                    update,
                    update.getCallbackQuery().getFrom()
            );
        }

        Message message = update.getMessage();
        String text = message != null && message.hasText() ? message.getText() : "";
        Contact contact = message != null ? message.getContact() : null;
        User telegramUser = message != null ? message.getFrom() : null;

        return new CommandContext(
                message != null ? message.getChatId() : null,
                text,
                contact,
                message,
                update,
                telegramUser
        );
    }

    public static CommandContext from(InboundEvent event) {
        if (event == null) {
            return null;
        }

        return new CommandContext(
                event.getChatId(),
                event.getPayload(),
                null,   // contact
                null,   // message
                null,   // update
                null    // telegramUser
        );
    }

    // 🔹 Удобные геттеры для быстрого доступа к имени пользователя
    public String getFirstName() {
        return telegramUser != null ? telegramUser.getFirstName() : "гость";
    }

    public String getLastName() {
        return telegramUser != null ? telegramUser.getLastName() : "";
    }

    public String getUsername() {
        return telegramUser != null ? telegramUser.getUserName() : "";
    }
}
