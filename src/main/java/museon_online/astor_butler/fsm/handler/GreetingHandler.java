package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

        log.info("🟢 [FSM] GREETING → start (chatId={})", chatId);

        String prompt = String.format(
                "Придумай короткое, тёплое и дружелюбное приветствие пользователю по имени %s, " +
                        "в стиле AI-дворецкого Astor Butler. Заверши текст призывом отправить контакт.", userName);

        try {
            log.debug("🧠 [AI] PROMPT: {}", prompt);
            String aiGreeting = alisaClient.ask(prompt);
            log.info("🎙️ [AI] RESPONSE: {}", aiGreeting);

            KeyboardButton shareContact = KeyboardButton.builder()
                    .text("📱 Поделиться контактом")
                    .requestContact(true)
                    .build();

            ReplyKeyboardMarkup kb = ReplyKeyboardMarkup.builder()
                    .keyboard(List.of(new KeyboardRow(List.of(shareContact))))
                    .resizeKeyboard(true)
                    .oneTimeKeyboard(true)
                    .build();

            sender.sendText(chatId, aiGreeting, kb);
            log.info("📤 [TG] Message sent to user (chatId={})", chatId);

            storage.setState(chatId, BotState.CONTACT);
            log.info("✅ [FSM] GREETING → next state: CONTACT");

        } catch (Exception e) {
            log.error("❌ [FSM] GREETING → AI error: {}", e.getMessage(), e);
            sender.sendText(chatId, "👋 Привет! Отправь свой контакт, чтобы продолжить.");
        }
    }
}