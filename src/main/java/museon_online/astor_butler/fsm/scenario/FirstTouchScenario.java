package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.domain.consent.ConsentVaultService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.kafka.UserEventProducer;
import museon_online.astor_butler.llm.OllamaClient;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirstTouchScenario {

    private static final String POLICY_URL = "https://michaelwelly.github.io/Astor_Butler_MVP/docs/policy.html";

    private final FSMStorage fsmStorage;
    private final OllamaClient ollamaClient;
    private final ConsentVaultService consentVaultService;
    private final UserEventProducer userEventProducer;
    private final TableBookingDraftStorage tableBookingDraftStorage;

    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        return signalFor(incoming, currentState, text) != null;
    }

    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        FirstTouchSignal signal = signalFor(incoming, currentState, text);
        if (signal == null) {
            throw new IllegalArgumentException("FirstTouchScenario does not support this message");
        }

        return switch (signal) {
            case START_COMMAND -> handleStart(incoming);
            case CONTACT_SHARED -> handleContact(incoming);
            case PRE_AUTH_TEXT -> handleConsentNudge(incoming, currentState, text);
        };
    }

    private FirstTouchSignal signalFor(IncomingMessage incoming, BotState currentState, String text) {
        if (hasContact(incoming)) {
            return FirstTouchSignal.CONTACT_SHARED;
        }
        if ("/start".equalsIgnoreCase(text)) {
            return FirstTouchSignal.START_COMMAND;
        }
        if (currentState == BotState.UNKNOWN && incoming.telegramUserId() != null) {
            return FirstTouchSignal.START_COMMAND;
        }
        if (currentState != null && currentState.waitsForConsentAndContact()) {
            return FirstTouchSignal.PRE_AUTH_TEXT;
        }
        return null;
    }

    private OutgoingMessage handleStart(IncomingMessage incoming) {
        tableBookingDraftStorage.clear(incoming.chatId());
        if (consentVaultService.hasGrantedPrivacyPolicy(incoming.telegramUserId())) {
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return OutgoingMessage.of(
                    incoming,
                    """
                    Я обновил начало диалога и оставил вас в главном меню.

                    Напишите свободно: забронировать стол, показать меню, показать ресторан внутри, рассказать о концепции или позвать менеджера.
                    """,
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("SAFE_RESTART", "OPEN_MENU")
            );
        }

        fsmStorage.setState(incoming.chatId(), BotState.CONSENT_REQUIRED);
        String response = "Нажимая кнопку \"Согласиться и поделиться контактом\", вы соглашаетесь с "
                + "<a href=\"" + POLICY_URL + "\">политикой обработки персональных данных</a>.";

        return OutgoingMessage.of(
                incoming,
                response,
                BotState.CONSENT_REQUIRED.name(),
                true,
                true,
                false,
                false,
                AdminAlert.none(),
                List.of("SAFE_RESTART", "REQUEST_CONTACT", "CONSENT_REQUIRED")
        );
    }

    private OutgoingMessage handleContact(IncomingMessage incoming) {
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        consentVaultService.grantPrivacyPolicyFromTelegramContact(incoming);
        return OutgoingMessage.of(
                incoming,
                "Спасибо, контакт получил. Теперь могу узнавать вас и вести сценарии аккуратнее.\n\nОткройте меню или напишите, что хотите забронировать.",
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("CONTACT_CAPTURED", "OPEN_MENU")
        );
    }

    private OutgoingMessage handleConsentNudge(IncomingMessage incoming, BotState currentState, String text) {
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

        return OutgoingMessage.of(
                incoming,
                answer.text(),
                BotState.CONSENT_REQUIRED.name(),
                false,
                true,
                false,
                false,
                AdminAlert.none(),
                List.of("PRE_AUTH_CONSENT_NUDGE", "REQUEST_CONTACT", "CONSENT_REQUIRED")
        );
    }

    private LlmAnswer askOrFallback(String prompt, String fallback) {
        try {
            String response = ollamaClient.ask(prompt);
            if (response == null || response.isBlank()) {
                log.warn("LLM returned blank response during first-touch scenario, fallback used");
                return new LlmAnswer(fallback, true);
            }
            return new LlmAnswer(response, false);
        } catch (Exception e) {
            log.warn("LLM fallback used during first-touch scenario: {}", e.getMessage());
            return new LlmAnswer(fallback, true);
        }
    }

    private boolean hasContact(IncomingMessage incoming) {
        return incoming.contactPhone() != null && !incoming.contactPhone().isBlank();
    }

    private record LlmAnswer(String text, boolean fallbackUsed) {
    }

    private enum FirstTouchSignal {
        START_COMMAND,
        CONTACT_SHARED,
        PRE_AUTH_TEXT
    }
}
