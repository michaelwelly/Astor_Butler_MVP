package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.preference.GuestPreference;
import museon_online.astor_butler.domain.preference.GuestPreferenceCategory;
import museon_online.astor_butler.domain.preference.GuestPreferenceCommand;
import museon_online.astor_butler.domain.preference.GuestPreferenceService;
import museon_online.astor_butler.domain.preference.GuestPreferenceStatus;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreferenceScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private GuestPreferenceService preferenceService;

    private PreferenceScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new PreferenceScenario(fsmStorage, preferenceService);
    }

    @Test
    void asksForTextWhenGuestOnlyOpensPreferences() {
        IncomingMessage incoming = telegram("предпочтения");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.PREFERENCE_COLLECT_TEXT.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly("PREFERENCE_MAP", "ASK_PREFERENCE_TEXT");
        verify(fsmStorage).setState(incoming.chatId(), BotState.PREFERENCE_COLLECT_TEXT);
    }

    @Test
    void savesCollectedPreferenceAndReturnsToMainMenu() {
        IncomingMessage incoming = telegram("люблю тихий стол у окна");
        when(preferenceService.classify(incoming.text())).thenReturn(GuestPreferenceCategory.SEATING);
        when(preferenceService.createPreference(any(GuestPreferenceCommand.class))).thenReturn(preference());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.PREFERENCE_COLLECT_TEXT, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("Запомнил", "тихий стол");
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly("PREFERENCE_MAP", "PREFERENCE_TEXT_RECEIVED", "RETURN_MAIN_MENU");
        assertThat(outgoing.metadata()).containsEntry("preferenceId", 55L);
        assertThat(outgoing.metadata()).containsEntry("category", "SEATING");
        assertThat(outgoing.metadata()).containsEntry("preferenceBoundary", "GUEST_PROVIDED_ONLY");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void savesDirectPreferenceIntent() {
        IncomingMessage incoming = telegram("запомни, я не ем острое");
        when(preferenceService.classify("я не ем острое")).thenReturn(GuestPreferenceCategory.FOOD);
        when(preferenceService.createPreference(any(GuestPreferenceCommand.class))).thenReturn(preference());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.actions()).containsExactly("PREFERENCE_MAP", "PREFERENCE_DIRECT_TEXT", "RETURN_MAIN_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    private IncomingMessage telegram(String text) {
        return IncomingMessage.telegram(
                1773317437L,
                1773317437L,
                351,
                284069875,
                text,
                null,
                "Наталья",
                "Поединенко",
                "Poedinenko",
                "ru",
                false,
                "284069875"
        );
    }

    private GuestPreference preference() {
        return new GuestPreference(
                55L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                GuestPreferenceCategory.SEATING,
                "люблю тихий стол у окна",
                "TELEGRAM",
                GuestPreferenceStatus.ACTIVE,
                1.0,
                BotState.PREFERENCE_COLLECT_TEXT.name(),
                "284069875",
                "{}",
                Instant.parse("2026-06-16T10:00:00Z"),
                Instant.parse("2026-06-16T10:00:00Z")
        );
    }
}
