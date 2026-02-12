package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import museon_online.astor_butler.alisa.AlisaClient;
//import museon_online.astor_butler.alisa.dto.AgentResponse;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.telegram.utils.TelegramSender;
import org.springframework.stereotype.Component;

/**
 * Fallback — осмысленный ответ AI,
 * когда сообщение не попало ни в один сценарий FSM.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FallbackHandler implements FSMHandler {

//    private final AlisaClient alisaClient;
    private final TelegramSender telegramSender;

    @Override
    public BotState getState() {
        return BotState.AI_FALLBACK;
    }

    @Override
    public void handle(CommandContext ctx) {
        Long chatId = ctx.getChatId();
        String userMessage = ctx.getMessageText();

        log.info(
                "🟢 [FSM] FALLBACK → start (chatId={}, text={})",
                chatId,
                userMessage
        );

        try {
            String prompt = """
                    Пользователь написал: "%s".

                    Ты — AI-дворецкий Astor Butler.
                    Ответь вежливо, коротко и по делу.
                    Если запрос не ясен — предложи посмотреть меню или задать вопрос.
                    """.formatted(userMessage);

            log.debug("🧠 [AI] PROMPT: {}", prompt);

//            AgentResponse ai = alisaClient.ask(prompt);
//
//            log.info(
//                    "🎙️ [AI] intent={}, confidence={}",
//                    ai.intent(),
//                    ai.confidence()
//            );

//            telegramSender.sendText(chatId, ai.text());

            log.info("📤 [TG] Fallback response sent (chatId={})", chatId);

        } catch (Exception e) {
            log.error("❌ [FSM] FALLBACK → AI error", e);

            telegramSender.sendText(
                    chatId,
                    "Извините, я сейчас не смог ответить. Попробуйте открыть меню 🙏"
            );
        }
    }
}