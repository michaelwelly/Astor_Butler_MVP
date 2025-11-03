package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.alisa.AlisaClient;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.telegram.TelegramSender;
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
        String userMessage = ctx.getMessageText();
        Long chatId = ctx.getChatId();

        log.info("🌀 FallbackHandler activated for chatId={}, text={}", chatId, userMessage);

        try {
            String prompt = String.format(
                    "Пользователь написал: \"%s\".\n" +
                            "Ответь от лица дворецкого Astor Butler — кратко, тепло, без лишних деталей, вежливо направь к кнопкам меню.",
                    userMessage
            );

            String reply = alisaClient.ask(prompt);
            telegramSender.sendText(chatId, reply);

        } catch (Exception e) {
            log.error("❌ Ошибка при обращении к Алисе: {}", e.getMessage(), e);
            telegramSender.sendText(chatId,
                    "Извините, сейчас я немного занят. Попробуйте написать позже 🙏");
        }
    }
}
