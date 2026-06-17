package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.telegram.TelegramGuestContextRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerHelpScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private TelegramGuestContextRepository guestContextRepository;

    private ManagerHelpScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ManagerHelpScenario(fsmStorage, guestContextRepository);
        ReflectionTestUtils.setField(scenario, "adminChatId", "100500");
    }

    @Test
    void asksForReasonWhenGuestOnlyCallsManager() {
        IncomingMessage incoming = telegram("менеджер");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.MANAGER_HELP_COLLECT_REASON.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly("MANAGER_HELP", "ASK_MANAGER_REASON");
        assertThat(outgoing.metadata()).containsEntry("scenario", "MANAGER_HELP");
        verify(fsmStorage).setState(incoming.chatId(), BotState.MANAGER_HELP_COLLECT_REASON);
    }

    @Test
    void sendsAdminAlertWhenReasonWasCollected() {
        IncomingMessage incoming = telegram("У нас задерживается бронь, нужен человек");
        when(guestContextRepository.recentMessages(incoming.chatId(), 5))
                .thenReturn(List.of(
                        "Хочу забронировать столик завтра на 20:00",
                        "Можно тихий стол в винной комнате?"
                ));

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.MANAGER_HELP_COLLECT_REASON, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.adminAlert().chatId()).isEqualTo("100500");
        assertThat(outgoing.adminAlert().text()).contains(
                "Astor Butler / manager help",
                "Наталья Поединенко",
                "У нас задерживается бронь",
                "Последний контекст диалога",
                "Хочу забронировать столик завтра на 20:00",
                "Можно тихий стол в винной комнате?"
        );
        assertThat(outgoing.actions()).containsExactly("MANAGER_HELP", "MANAGER_HELP_REASON_RECEIVED", "ADMIN_ALERT", "RETURN_MAIN_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void supportsDirectManagerHelpRequestFromMainMenu() {
        IncomingMessage incoming = telegram("позови менеджера, хочу обсудить банкет");
        when(guestContextRepository.recentMessages(incoming.chatId(), 5)).thenReturn(List.of());

        assertThat(scenario.supports(incoming, BotState.READY_FOR_DIALOG, incoming.text())).isTrue();

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.actions()).containsExactly("MANAGER_HELP", "MANAGER_HELP_DIRECT_REQUEST", "ADMIN_ALERT", "RETURN_MAIN_MENU");
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
