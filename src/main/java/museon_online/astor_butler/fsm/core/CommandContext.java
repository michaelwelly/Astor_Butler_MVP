package museon_online.astor_butler.fsm.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Data
@AllArgsConstructor
public class CommandContext {
    private final Long chatId;
    private final String messageText;
    private final Contact contact;
    private final Message message;
    private final Update update;

    // 🔹 Вспомогательный конструктор для универсальной сборки контекста
    public static CommandContext from(Update update) {
        Message message = update.getMessage();
        String text = message != null && message.hasText() ? message.getText() : "";
        Contact contact = message != null ? message.getContact() : null;

        return new CommandContext(
                message != null ? message.getChatId() : null,
                text,
                contact,
                message,
                update
        );
    }
}