package museon_online.astor_butler.fsm.core;

import lombok.Value;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Value
public class CommandContext {
    Long    userId;
    String  text;
    String  callbackData;
    Contact contact;
    Message message;
    Update update;
}