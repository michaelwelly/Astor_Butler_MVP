package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.llm.OllamaClient;
import museon_online.astor_butler.telegram.utils.TelegramSender;
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

        private static final String POLICY_URL = "https://michaelwelly.github.io/Astor_Butler_MVP/docs/policy.html";

        private final TelegramSender sender;
        private final FSMStorage storage;
        private final OllamaClient ollamaClient;

        @Override
        public BotState getState() {
            return BotState.GREETING;
        }

        @Override
        public void handle(CommandContext ctx) {
            Long chatId = ctx.getChatId();
            String userName = ctx.getFirstName();

            storage.setState(chatId, BotState.GREETING);

            log.info("🟢 [FSM] State set to GREETING (chatId={})", chatId);

            String prompt = String.format(
                    "Ты вежливый цифровой дворецкий. " +
                    "Одним предложением поприветствуй пользователя по имени %s.",
                    userName
            );

            try {
                log.info("🧠 [LLM] Sending prompt (chatId={})", chatId);
                log.debug("🧠 [LLM] Prompt text: {}", prompt);

                long startedAt = System.nanoTime();

                String llmResponse = ollamaClient.ask(prompt);

                long durationMs = (System.nanoTime() - startedAt) / 1_000_000;

                log.info(
                        "🧠 [LLM] Response received (chatId={}, duration={} ms)",
                        chatId,
                        durationMs
                );

                log.debug(
                        "🧠 [LLM] Response text: {}",
                        llmResponse.length() > 300
                                ? llmResponse.substring(0, 300) + "…"
                                : llmResponse
                );

                String finalText =
                        llmResponse + "\n\n" +
                        "Чтобы продолжить, отправьте, пожалуйста, свой контакт.\n\n" +
                        "Продолжая, вы соглашаетесь с " +
                        "<a href=\"" + POLICY_URL + "\">политикой обработки персональных данных</a>.";

                KeyboardButton shareContact = KeyboardButton.builder()
                        .text("📱 Поделиться контактом")
                        .requestContact(true)
                        .build();

                ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
                        .keyboard(List.of(new KeyboardRow(List.of(shareContact))))
                        .resizeKeyboard(true)
                        .oneTimeKeyboard(true)
                        .build();

                sender.sendHtml(chatId, finalText, keyboard);

                log.info("✅ [FSM] GREETING message sent, waiting for contact");

            } catch (Exception e) {
                log.error("❌ [FSM] GREETING → LLM error", e);

                String fallbackText =
                        "\n\n" +
                        "Чтобы продолжить, отправьте, пожалуйста, свой контакт.\n\n" +
                        "Продолжая, вы соглашаетесь с " +
                        "<a href=\"" + POLICY_URL + "\">политикой обработки персональных данных</a>.";

                KeyboardButton shareContact = KeyboardButton.builder()
                        .text("📱 Поделиться контактом")
                        .requestContact(true)
                        .build();

                ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
                        .keyboard(List.of(new KeyboardRow(List.of(shareContact))))
                        .resizeKeyboard(true)
                        .oneTimeKeyboard(true)
                        .build();

                sender.sendText(chatId, fallbackText, keyboard);
            }
        }
    }