package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.consent.ConsentVaultService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.kafka.UserEventProducer;
import museon_online.astor_butler.llm.OllamaClient;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirstTouchScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private OllamaClient ollamaClient;

    @Mock
    private ConsentVaultService consentVaultService;

    @Mock
    private UserEventProducer userEventProducer;

    @Mock
    private TableBookingDraftStorage tableBookingDraftStorage;

    private FirstTouchScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new FirstTouchScenario(fsmStorage, ollamaClient, consentVaultService, userEventProducer, tableBookingDraftStorage);
    }

    @Test
    void startsByRequestingContactAndConsent() {
        IncomingMessage incoming = telegram("/start", null);

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.UNKNOWN, "/start");

        assertThat(outgoing.nextState()).isEqualTo(BotState.CONSENT_REQUIRED.name());
        assertThat(outgoing.requestContact()).isTrue();
        assertThat(outgoing.html()).isTrue();
        assertThat(outgoing.text()).contains("Согласиться и поделиться контактом");
        assertThat(outgoing.actions()).containsExactly("SAFE_RESTART", "REQUEST_CONTACT", "CONSENT_REQUIRED");
        verify(fsmStorage).setState(421441838L, BotState.CONSENT_REQUIRED);
        verify(tableBookingDraftStorage).clear(incoming.chatId());
    }

    @Test
    void startForKnownGuestResetsRuntimeAndReturnsReady() {
        IncomingMessage incoming = telegram("/start", null);
        when(consentVaultService.hasGrantedPrivacyPolicy(incoming.telegramUserId())).thenReturn(true);

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION, "/start");

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.removeKeyboard()).isTrue();
        assertThat(outgoing.text()).contains("обновил начало диалога", "главном меню");
        assertThat(outgoing.actions()).containsExactly("SAFE_RESTART", "OPEN_MENU");
        verify(tableBookingDraftStorage).clear(incoming.chatId());
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void unknownTelegramGuestRequestsContactAndConsentWithoutStartCommand() {
        IncomingMessage incoming = telegram("Привет", null);

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.UNKNOWN, "Привет");

        assertThat(outgoing.nextState()).isEqualTo(BotState.CONSENT_REQUIRED.name());
        assertThat(outgoing.requestContact()).isTrue();
        assertThat(outgoing.actions()).containsExactly("SAFE_RESTART", "REQUEST_CONTACT", "CONSENT_REQUIRED");
        verify(fsmStorage).setState(421441838L, BotState.CONSENT_REQUIRED);
    }

    @Test
    void storesConsentAndOpensMenuAfterContact() {
        IncomingMessage incoming = telegram("", "+79990000000");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.CONSENT_REQUIRED, "");

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.removeKeyboard()).isTrue();
        assertThat(outgoing.actions()).containsExactly("CONTACT_CAPTURED", "OPEN_MENU");
        verify(fsmStorage).setState(421441838L, BotState.READY_FOR_DIALOG);
        verify(consentVaultService).grantPrivacyPolicyFromTelegramContact(incoming);
    }

    @Test
    void keepsContactStateWhenGuestWritesBeforeConsent() {
        IncomingMessage incoming = telegram("А можно без контакта?", null);
        when(ollamaClient.ask(any())).thenReturn("Можно, но сценарий продолжится после кнопки контакта.");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.CONSENT_REQUIRED, "А можно без контакта?");

        assertThat(outgoing.nextState()).isEqualTo(BotState.CONSENT_REQUIRED.name());
        assertThat(outgoing.requestContact()).isTrue();
        assertThat(outgoing.actions()).containsExactly(
                "PRE_AUTH_CONSENT_NUDGE",
                "REQUEST_CONTACT",
                "CONSENT_REQUIRED"
        );
        assertThat(outgoing.text()).contains("кнопки контакта");
        verify(userEventProducer).publishLlmResponse(
                eq(incoming),
                eq(BotState.CONSENT_REQUIRED),
                eq("PRE_AUTH_CONSENT_NUDGE"),
                any(),
                eq("Можно, но сценарий продолжится после кнопки контакта."),
                eq(false)
        );
    }

    private IncomingMessage telegram(String text, String phone) {
        return IncomingMessage.telegram(
                421441838L,
                284069847L,
                10,
                20,
                text,
                phone,
                "Наталья",
                "Тестова",
                "natalia_test",
                "ru",
                false,
                "20"
        );
    }
}
