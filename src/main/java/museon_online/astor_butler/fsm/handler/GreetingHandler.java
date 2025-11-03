package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.alisa.AlisaClient;
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
    private final AlisaClient alisaClient;

    @Override
    public BotState getState() {
        return BotState.GREETING;
    }


    @Override
    public void handle(CommandContext ctx) {
        Long chatId = ctx.getChatId();
        String userName = ctx.getFirstName();

        String prompt = String.format(
                "Придумай короткое, тёплое и дружелюбное приветствие пользователю по имени %s, " +
                        "в стиле AI-дворецкого Astor Butler. Заверши текст призывом отправить контакт, " +
                        "чтобы начать работу. Примеры: 'Привет, %s! Рад встрече. Отправь контакт, чтобы я знал, кто ты.' " +
                        "или 'Здравствуйте, %s! Astor Butler к вашим услугам — поделитесь контактом для начала знакомства.'",
                userName, userName, userName
        );

        String aiGreeting;
        try {
            aiGreeting = alisaClient.ask(prompt);
        } catch (Exception e) {
            aiGreeting = "👋 Привет, " + userName + "! Отправь свой контакт, чтобы продолжить.";
        }

        // 📱 создаём клавиатуру
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

        // 📤 отправляем AI-приветствие
        sender.sendText(chatId, aiGreeting, kb);

        // 🗂️ переводим FSM в состояние CONTACT
        storage.setState(chatId, BotState.CONTACT);
    }
}