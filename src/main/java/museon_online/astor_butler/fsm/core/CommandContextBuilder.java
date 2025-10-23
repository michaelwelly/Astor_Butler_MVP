package museon_online.astor_butler.fsm.core;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class CommandContextBuilder {

    public CommandContext from(Update update) {
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