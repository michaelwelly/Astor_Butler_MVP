package museon_online.astor_butler.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.llm.OllamaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageGatewayService {

    private static final String POLICY_URL = "https://michaelwelly.github.io/Astor_Butler_MVP/docs/policy.html";

    private final FSMStorage fsmStorage;
    private final OllamaClient ollamaClient;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    public OutgoingMessage handle(IncomingMessage incoming) {
        if (incoming == null || incoming.chatId() == null) {
            throw new IllegalArgumentException("Incoming message must contain chatId for current MVP flow");
        }

        String text = normalize(incoming.text());
        BotState currentState = resolveState(incoming.chatId());

        log.info(
                "Message gateway received channel={}, chatId={}, state={}, text={}",
                incoming.channel(),
                incoming.chatId(),
                currentState,
                text
        );

        if (incoming.contactPhone() != null && !incoming.contactPhone().isBlank()) {
            fsmStorage.setState(incoming.chatId(), BotState.MENU);
            return OutgoingMessage.of(
                    incoming,
                    "Спасибо, контакт получил. Теперь могу узнавать вас и вести сценарии аккуратнее.\n\nОткройте меню или напишите, что хотите забронировать.",
                    BotState.MENU.name(),
                    false,
                    false,
                    false,
                    AdminAlert.none(),
                    List.of("CONTACT_CAPTURED", "OPEN_MENU")
            );
        }

        if ("/start".equalsIgnoreCase(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.CONTACT);
            return greeting(incoming);
        }

        if (currentState == BotState.CONTACT) {
            return OutgoingMessage.of(
                    incoming,
                    "Чтобы продолжить сценарий, поделитесь контактом кнопкой ниже. Так Astor Butler сможет связать Telegram с профилем гостя и согласием на обработку данных.",
                    BotState.CONTACT.name(),
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("REQUEST_CONTACT", "CONSENT_REQUIRED")
            );
        }

        if (text.isBlank()) {
            return fallback(incoming, currentState, "Empty message");
        }

        if (isMenuRequest(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.MENU);
            return OutgoingMessage.of(
                    incoming,
                    "Меню MVP: бронирование, афиша, таймлайн, медиа и связь с менеджером. Пока это первый FSM-срез, дальше наполним сценариями.",
                    BotState.MENU.name(),
                    false,
                    false,
                    false,
                    AdminAlert.none(),
                    List.of("SHOW_MENU")
            );
        }

        return aiAssistedReply(incoming, currentState, text);
    }

    private OutgoingMessage greeting(IncomingMessage incoming) {
        String name = incoming.firstName() == null || incoming.firstName().isBlank()
                ? "гость"
                : incoming.firstName();
        String prompt = "Ты вежливый цифровой дворецкий Astor Butler. "
                + "Одним коротким предложением поприветствуй пользователя по имени " + name
                + " и скажи, что поможешь с бронированием, меню и событиями.";

        String greetingText = askOrFallback(prompt, "Здравствуйте, " + name + ". Я Astor Butler, помогу с бронированием, меню и событиями.");
        String response = greetingText + "\n\n"
                + "Чтобы продолжить, отправьте, пожалуйста, свой контакт.\n\n"
                + "Продолжая, вы соглашаетесь с "
                + "<a href=\"" + POLICY_URL + "\">политикой обработки персональных данных</a>.";

        return OutgoingMessage.of(
                incoming,
                response,
                BotState.CONTACT.name(),
                true,
                true,
                false,
                AdminAlert.none(),
                List.of("REQUEST_CONTACT", "CONSENT_REQUIRED")
        );
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

            fsmStorage.setState(incoming.chatId(), BotState.AI_FALLBACK);
            return OutgoingMessage.of(
                    incoming,
                    aiText,
                    BotState.AI_FALLBACK.name(),
                    false,
                    false,
                    false,
                    AdminAlert.none(),
                    List.of("AI_RESPONSE")
            );
        } catch (Exception e) {
            log.error("LLM failed for chatId={}", incoming.chatId(), e);
            return fallback(incoming, currentState, e.getClass().getSimpleName());
        }
    }

    private OutgoingMessage fallback(IncomingMessage incoming, BotState currentState, String reason) {
        fsmStorage.setState(incoming.chatId(), BotState.AI_FALLBACK);
        String userText = "Я не смог уверенно разобрать запрос. Я передам это администратору, а вы можете написать проще: бронирование, меню, афиша или менеджер.";

        return OutgoingMessage.of(
                incoming,
                userText,
                BotState.AI_FALLBACK.name(),
                false,
                false,
                true,
                adminAlert(incoming, currentState, reason),
                List.of("FALLBACK", "ADMIN_ALERT")
        );
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

    private String askOrFallback(String prompt, String fallback) {
        try {
            String response = ollamaClient.ask(prompt);
            return response == null || response.isBlank() ? fallback : response;
        } catch (Exception e) {
            log.warn("Greeting LLM fallback used: {}", e.getMessage());
            return fallback;
        }
    }

    private boolean isMenuRequest(String text) {
        String lower = text.toLowerCase();
        return lower.contains("меню") || lower.equals("/menu") || lower.contains("menu");
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
