package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.consent.ConsentVaultService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FirstTouchScenario implements FsmScenario {

    private static final String POLICY_URL = "https://michaelwelly.github.io/Astor_Butler_MVP/docs/policy.html";

    private final FSMStorage fsmStorage;
    private final ConsentVaultService consentVaultService;
    private final TableBookingDraftStorage tableBookingDraftStorage;

    public String id() {
        return "FIRST_TOUCH";
    }

    public int priority() {
        return 10;
    }

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
                    "",
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("SAFE_RESTART", "PREVIEW_REFRESHED", "OPEN_MENU")
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
                "Спасибо, контакт получил. Я на связи: выберите действие кнопкой ниже или напишите запрос своими словами.",
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
        return OutgoingMessage.of(
                incoming,
                "Для брони мне нужен контакт, чтобы команда AERIS могла подтвердить детали. Нажмите «Согласиться и поделиться контактом» ниже.",
                BotState.CONSENT_REQUIRED.name(),
                false,
                true,
                false,
                false,
                AdminAlert.none(),
                List.of("PRE_AUTH_CONSENT_NUDGE", "REQUEST_CONTACT", "CONSENT_REQUIRED")
        );
    }

    private boolean hasContact(IncomingMessage incoming) {
        return incoming.contactPhone() != null && !incoming.contactPhone().isBlank();
    }

    public boolean owns(BotState state) {
        return state != null && state.canonical() == BotState.CONSENT_REQUIRED;
    }

    public boolean sideEffecting() {
        return true;
    }

    private enum FirstTouchSignal {
        START_COMMAND,
        CONTACT_SHARED,
        PRE_AUTH_TEXT
    }
}
