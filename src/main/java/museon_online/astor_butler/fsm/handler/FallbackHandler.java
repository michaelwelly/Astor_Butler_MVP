package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.alisa.AlisaClient;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.telegram.utils.TelegramSender;
import org.springframework.stereotype.Component;

/**
 * Обрабатывает любые неожиданные сообщения —
 * делегирует Яндекс LLM для генерации осмысленного ответа.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FallbackHandler implements FSMHandler {

   private final AlisaClient alisaClient;
   private final TelegramSender telegramSender;

    @Override
    public BotState getState() {
        return BotState.AI_FALLBACK;
    }

    @Override
    public void handle(CommandContext ctx) {
        Long chatId = ctx.getChatId();
        String userMessage = ctx.getMessageText();

        log.info("🟢 [FSM] FALLBACK → start (chatId={}, text={})", chatId, userMessage);

        try {
            String prompt = String.format(
                    "Пользователь написал: \"%s\".\n" +
                            "Ответь от лица дворецкого Astor Butler — кратко, тепло и дружелюбно, предложи открыть меню.",
                    userMessage
            );

            log.debug("🧠 [AI] PROMPT: {}", prompt);
            String reply = alisaClient.ask(prompt);
            log.info("🎙️ [AI] RESPONSE: {}", reply);

            telegramSender.sendText(chatId, reply);
            log.info("📤 [TG] Message sent to user (chatId={})", chatId);

        } catch (Exception e) {
            log.error("❌ [FSM] FALLBACK → AI error: {}", e.getMessage(), e);
            telegramSender.sendText(chatId,
                    "Извините, сейчас я немного занят. Попробуйте написать позже 🙏");
        }
    }
}
