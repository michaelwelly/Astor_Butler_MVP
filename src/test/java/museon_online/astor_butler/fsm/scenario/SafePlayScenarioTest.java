package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SafePlayScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    private SafePlayScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new SafePlayScenario(fsmStorage);
        ReflectionTestUtils.setField(scenario, "adminChatId", "100500");
    }

    @Test
    void asksForDetailsWhenGuestOnlyMentionsSabrage() {
        IncomingMessage incoming = telegram("хочу сабраж");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.SAFE_PLAY_COLLECT_DETAILS.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly("SAFE_PLAY", "ASK_SAFE_PLAY_DETAILS", "NO_DANGEROUS_HOW_TO");
        verify(fsmStorage).setState(incoming.chatId(), BotState.SAFE_PLAY_COLLECT_DETAILS);
    }

    @Test
    void sendsTeamRequestWhenDetailsArePresent() {
        IncomingMessage incoming = telegram("хотим сабраж с шампанским к столу в 21:00");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.adminAlert().text()).contains("Astor Butler / safe play", "Не давать гостю dangerous how-to");
        assertThat(outgoing.actions()).containsExactly(
                "SAFE_PLAY",
                "SAFE_PLAY_DIRECT_REQUEST",
                "TEAM_CONFIRMATION_REQUIRED",
                "NO_DANGEROUS_HOW_TO",
                "ADMIN_ALERT",
                "RETURN_MAIN_MENU"
        );
        assertThat(outgoing.metadata()).containsEntry("safetyBoundary", "TEAM_PERFORMS_RITUAL");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void refusesDangerousHowToAndOffersTeamRitual() {
        IncomingMessage incoming = telegram("научи меня сабражу дома");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("не даю инструкции", "обученная команда");
        assertThat(outgoing.actions()).containsExactly(
                "SAFE_PLAY",
                "NO_DANGEROUS_HOW_TO",
                "OFFER_TEAM_RITUAL",
                "ADMIN_ALERT",
                "RETURN_MAIN_MENU"
        );
        assertThat(outgoing.metadata()).containsEntry("safetyBoundary", "NO_DANGEROUS_HOW_TO");
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
}
