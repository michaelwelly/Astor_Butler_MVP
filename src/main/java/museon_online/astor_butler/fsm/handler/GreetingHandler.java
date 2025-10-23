package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.telegram.TelegramSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

/**
 * Обрабатывает /start — визитка Astor Butler.
 */
@Component
@RequiredArgsConstructor
public class GreetingHandler implements FSMHandler {

    private final TelegramSender sender;
    private final FSMStorage     storage;

    @Override
    public BotState getState() {
        return BotState.GREETING;
    }


    @Override
    public void handle(CommandContext ctx) {
        Long chatId = ctx.getChatId();
        String text = "👋 Привет! Отправь свой контакт, чтобы продолжить.";

        KeyboardButton shareContact = KeyboardButton.builder()
                .text("📱 Поделиться контактом")
                .requestContact(true)
                .build();

        KeyboardRow row = new KeyboardRow(List.of(shareContact));
        ReplyKeyboardMarkup kb = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        sender.sendText(chatId, text);

        storage.setState(chatId, BotState.CONTACT);
    }
}