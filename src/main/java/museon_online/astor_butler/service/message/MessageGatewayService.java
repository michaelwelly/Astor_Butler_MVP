package museon_online.astor_butler.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.domain.consent.ConsentVaultService;
import museon_online.astor_butler.domain.telegram.TelegramIntakeService;
import museon_online.astor_butler.fsm.core.BotState;
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

    private static final String POLICY_URL = "https://michaelwelly.github.io/Astor_Butler_MVP/docs/policy.html";

    private final FSMStorage fsmStorage;
    private final OllamaClient ollamaClient;
    private final TelegramIntakeService telegramIntakeService;
    private final ConsentVaultService consentVaultService;
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

        if (incoming.contactPhone() != null && !incoming.contactPhone().isBlank()) {
            fsmStorage.setState(incoming.chatId(), BotState.MENU);
            consentVaultService.grantPrivacyPolicyFromTelegramContact(incoming);
            return finish(incoming, currentState, OutgoingMessage.of(
                    incoming,
                    "Спасибо, контакт получил. Теперь могу узнавать вас и вести сценарии аккуратнее.\n\nОткройте меню или напишите, что хотите забронировать.",
                    BotState.MENU.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("CONTACT_CAPTURED", "OPEN_MENU")
            ));
        }

        if ("/start".equalsIgnoreCase(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.CONTACT);
            return finish(incoming, currentState, greeting(incoming));
        }

        if (currentState == BotState.CONTACT) {
            return contactConsentNudge(incoming, currentState, text);
        }

        if (text.isBlank()) {
            return fallback(incoming, currentState, "Empty message");
        }

        if (isMenuRequest(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.MENU);
            return finish(incoming, currentState, OutgoingMessage.of(
                    incoming,
                    "Меню MVP: бронирование, афиша, таймлайн, медиа и связь с менеджером. Пока это первый FSM-срез, дальше наполним сценариями.",
                    BotState.MENU.name(),
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

    private OutgoingMessage greeting(IncomingMessage incoming) {
        String name = incoming.firstName() == null || incoming.firstName().isBlank()
                ? "гость"
                : incoming.firstName();
        String response = "Здравствуйте, " + name + ". Я Astor Butler, ваш цифровой дворецкий для бронирований, меню и событий.\n\n"
                + "Чтобы продолжить, нажмите кнопку \"Согласиться и поделиться контактом\".\n\n"
                + "Нажимая кнопку, вы соглашаетесь с "
                + "<a href=\"" + POLICY_URL + "\">политикой обработки персональных данных</a>.";

        return OutgoingMessage.of(
                incoming,
                response,
                BotState.CONTACT.name(),
                true,
                true,
                false,
                false,
                AdminAlert.none(),
                List.of("REQUEST_CONTACT", "CONSENT_REQUIRED")
        );
    }

    private OutgoingMessage contactConsentNudge(IncomingMessage incoming, BotState currentState, String text) {
        String prompt = """
                        Ты Astor Butler, цифровой дворецкий. Гость еще не нажал кнопку согласия и не поделился контактом.
                        Твоя задача: очень коротко, спокойно и элегантно ответить на реплику гостя и вернуть его к кнопке.
                        Не продолжай бронирование, не собирай данные, не обещай действие менеджера.
                        Обязательно упомяни, что для продолжения нужно нажать кнопку "Согласиться и поделиться контактом".
                        Максимум 2 коротких предложения.

                        Реплика гостя: "%s"
                        """.formatted(text);
        LlmAnswer answer = askOrFallback(
                prompt,
                "Понимаю. Чтобы продолжить, нажмите кнопку \"Согласиться и поделиться контактом\" ниже."
        );
        userEventProducer.publishLlmResponse(
                incoming,
                currentState,
                "PRE_AUTH_CONSENT_NUDGE",
                prompt,
                answer.text(),
                answer.fallbackUsed()
        );

        return finish(incoming, currentState, OutgoingMessage.of(
                incoming,
                answer.text(),
                BotState.CONTACT.name(),
                false,
                true,
                false,
                false,
                AdminAlert.none(),
                List.of("PRE_AUTH_CONSENT_NUDGE", "REQUEST_CONTACT", "CONSENT_REQUIRED")
        ));
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

    private LlmAnswer askOrFallback(String prompt, String fallback) {
        try {
            String response = ollamaClient.ask(prompt);
            if (response == null || response.isBlank()) {
                log.warn("LLM returned blank response, fallback used");
                return new LlmAnswer(fallback, true);
            }
            return new LlmAnswer(response, false);
        } catch (Exception e) {
            log.warn("LLM fallback used: {}", e.getMessage());
            return new LlmAnswer(fallback, true);
        }
    }

    private record LlmAnswer(String text, boolean fallbackUsed) {
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
