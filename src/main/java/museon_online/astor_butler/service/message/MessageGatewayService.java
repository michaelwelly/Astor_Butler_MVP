package museon_online.astor_butler.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.domain.telegram.TelegramIntakeService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.scenario.FirstTouchScenario;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.kafka.UserEventProducer;
import museon_online.astor_butler.llm.OllamaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageGatewayService {

    private final FSMStorage fsmStorage;
    private final OllamaClient ollamaClient;
    private final TelegramIntakeService telegramIntakeService;
    private final FirstTouchScenario firstTouchScenario;
    private final UserEventProducer userEventProducer;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    @Value("${telegram.analytics.chat-id:}")
    private String analyticsChatId;

    @Value("${astor.message.log-conversations-enabled:true}")
    private boolean logConversationsEnabled;

    public OutgoingMessage handle(IncomingMessage incoming) {
        if (incoming == null || incoming.chatId() == null) {
            throw new IllegalArgumentException("Incoming message must contain chatId for current MVP flow");
        }

        String text = normalize(incoming.text());
        BotState currentState = resolveState(incoming.chatId());
        telegramIntakeService.capture(incoming);

        if (logConversationsEnabled) {
            log.info(
                    "Message gateway received channel={}, chatId={}, state={}, text={}",
                    incoming.channel(),
                    incoming.chatId(),
                    currentState,
                    text
            );
        } else {
            log.debug(
                    "Message gateway received channel={}, chatId={}, state={}",
                    incoming.channel(),
                    incoming.chatId(),
                    currentState
            );
        }

        if (isAdminChat(incoming.chatId())) {
            return finish(incoming, currentState, OutgoingMessage.of(
                    incoming,
                    "Admin chat online. Я вижу этот чат как служебный канал Astor Butler: сообщения сохраняю, Kafka-события публикую, в гостевой FSM-сценарий этот чат не отправляю.",
                    currentState.name(),
                    false,
                    false,
                    false,
                    false,
                    AdminAlert.none(),
                    List.of("ADMIN_CHAT_CHECK", "SKIP_GUEST_FSM")
            ));
        }

        if (firstTouchScenario.supports(incoming, currentState, text)) {
            return finish(incoming, currentState, firstTouchScenario.handle(incoming, currentState, text));
        }

        if (text.isBlank()) {
            return fallback(incoming, currentState, "Empty message");
        }

        if (isMenuRequest(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return finish(incoming, currentState, OutgoingMessage.of(
                    incoming,
                    "Меню MVP: бронирование, афиша, таймлайн, медиа и связь с менеджером. Пока это первый FSM-срез, дальше наполним сценариями.",
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("SHOW_MENU")
            ));
        }

        return aiAssistedReply(incoming, currentState, text);
    }

    private OutgoingMessage aiAssistedReply(IncomingMessage incoming, BotState currentState, String text) {
        String prompt = """
                Ты AI-адаптер Astor Butler. Telegram является только UI, бизнес-логика живет в FSM.
                Ответь пользователю коротко и вежливо.
                Если запрос похож на бронирование, мягко попроси дату, время, количество гостей и контакт.
                Если запрос неясен, честно скажи, что уточнишь у менеджера.

                Текущее FSM-состояние: %s
                Сообщение пользователя: "%s"
                """.formatted(currentState, text);

        try {
            String aiText = ollamaClient.ask(prompt);
            if (aiText == null || aiText.isBlank()) {
                return fallback(incoming, currentState, "LLM returned blank response");
            }
            userEventProducer.publishLlmResponse(
                    incoming,
                    currentState,
                    "AI_RESPONSE",
                    prompt,
                    aiText,
                    false
            );

            fsmStorage.setState(incoming.chatId(), BotState.AI_FALLBACK);
            return finish(incoming, currentState, OutgoingMessage.of(
                    incoming,
                    aiText,
                    BotState.AI_FALLBACK.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("AI_RESPONSE")
            ));
        } catch (Exception e) {
            log.error("LLM failed for chatId={}", incoming.chatId(), e);
            return fallback(incoming, currentState, e.getClass().getSimpleName());
        }
    }

    private OutgoingMessage fallback(IncomingMessage incoming, BotState currentState, String reason) {
        fsmStorage.setState(incoming.chatId(), BotState.AI_FALLBACK);
        String userText = "Я не смог уверенно разобрать запрос. Я передам это администратору, а вы можете написать проще: бронирование, меню, афиша или менеджер.";

        return finish(incoming, currentState, OutgoingMessage.of(
                incoming,
                userText,
                BotState.AI_FALLBACK.name(),
                false,
                false,
                true,
                true,
                adminAlert(incoming, currentState, reason),
                List.of("FALLBACK", "ADMIN_ALERT")
        ));
    }

    private OutgoingMessage finish(IncomingMessage incoming, BotState previousState, OutgoingMessage outgoing) {
        userEventProducer.publishIncomingMessage(incoming, previousState, outgoing);
        return outgoing;
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState currentState, String reason) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }

        String text = """
                Astor Butler fallback
                Channel: %s
                Chat: %s
                User: %s
                State: %s
                Reason: %s
                Text: %s
                Correlation: %s
                """.formatted(
                incoming.channel(),
                incoming.chatId(),
                incoming.username() == null ? "" : incoming.username(),
                currentState,
                reason,
                incoming.text() == null ? "" : incoming.text(),
                incoming.correlationId() == null ? "" : incoming.correlationId()
        );
        return new AdminAlert(true, adminChatId, text);
    }

    private BotState resolveState(Long chatId) {
        BotState current = fsmStorage.getState(chatId);
        if (current != null) {
            return current;
        }
        fsmStorage.setState(chatId, BotState.UNKNOWN);
        return BotState.UNKNOWN;
    }

    private boolean isMenuRequest(String text) {
        String lower = text.toLowerCase();
        return lower.contains("меню") || lower.equals("/menu") || lower.contains("menu");
    }

    private boolean isAdminChat(Long chatId) {
        if (chatId == null) {
            return false;
        }
        String value = chatId.toString();
        return value.equals(adminChatId) || value.equals(analyticsChatId);
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
