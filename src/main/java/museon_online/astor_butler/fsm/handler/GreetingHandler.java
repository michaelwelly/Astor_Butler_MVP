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
    public boolean canHandle(CommandContext ctx) {
        // «Чистый» /start независимо от предыдущего состояния
        return "/start".equals(ctx.getText());
    }

    @Override
    public void handle(CommandContext ctx) {

        Long chatId = ctx.getUserId();

        String text = """
                🎩 <b>Добро пожаловать в AERIS, милорд.</b>

                Я <b>Astor Butler</b> — ваш цифровой дворецкий. Уже умею:
                 • рекомендовать коктейли,
                 • запоминать ваши вкусы,
                 • а вскоре — бронировать столы.

                Прежде чем мы продолжим, прошу подтвердить номер телефона —
                под покровом нашей <a href="https://aeris.bar/privacy">Политики конфиденциальности</a>.
                """;

        KeyboardButton shareContact = KeyboardButton.builder()
                .text("📞 Поделиться контактом")
                .requestContact(true)
                .build();

        KeyboardRow row = new KeyboardRow(List.of(shareContact));
        ReplyKeyboardMarkup kb = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
        // -------------------------------------------

        sender.sendHtml(chatId, text, kb);

        storage.setState(chatId.toString(), BotState.WAITING_FOR_PHONE.name());
    }
}