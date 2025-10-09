package museon_online.astor_butler.fsm.core;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class CommandContextBuilder {

    public CommandContext from(Update update) {
        Message message = update.getMessage();
        CallbackQuery callback = update.getCallbackQuery();

        String text = message != null ? message.getText() : null;
        String callbackData = callback != null ? callback.getData() : null;
        Contact contact = message != null ? message.getContact() : null;

        Long userId = null;
        if (message != null && message.getFrom() != null) {
            userId = message.getFrom().getId();
        } else if (callback != null && callback.getFrom() != null) {
            userId = callback.getFrom().getId();
        }

        return new CommandContext(
                userId,
                text,
                callbackData,
                contact,
                message,
                update
        );
    }
}