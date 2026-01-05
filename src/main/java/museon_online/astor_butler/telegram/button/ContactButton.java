package museon_online.astor_butler.telegram.button;

import museon_online.astor_butler.telegram.utils.BotButton;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class ContactButton implements BotButton {

    @Override
    public InlineKeyboardMarkup buildButton() {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("☎️ Контакты");
        button.setCallbackData("/contact_handler");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(button)));
        return markup;
    }

    @Override
    public String getCommand() {
        return "/contact_handler";
    }

    @Override
    public String getDescription() {
        return "Получить контактные данные пользователя";
    }
}