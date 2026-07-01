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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MainMenuScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    private MainMenuScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new MainMenuScenario(fsmStorage);
    }

    @Test
    void showsMainMenuFromReadyState() {
        IncomingMessage incoming = telegram("меню");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains(
                "забронировать стол",
                "сабраж",
                "чаевые, донат, аукцион или мерч"
        );
        assertThat(outgoing.actions()).containsExactly("MAIN_MENU", "SHOW_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void supportsOnlyExplicitMainMenuCommands() {
        IncomingMessage incoming = telegram("главное меню");

        assertThat(scenario.supports(incoming, BotState.READY_FOR_DIALOG, incoming.text())).isTrue();
        assertThat(scenario.supports(incoming, BotState.AI_FALLBACK, "main menu")).isTrue();
        assertThat(scenario.supports(incoming, BotState.READY_FOR_DIALOG, "что умеешь?")).isFalse();
    }

    @Test
    void doesNotStealProductBranches() {
        IncomingMessage incoming = telegram("меню");
        String[] productRequests = {
                "хочу оставить чаевые 1000",
                "хочу поддержать благотворительный проект",
                "ставлю 20000 за картину",
                "сколько собрали?",
                "хочу закрыть зал на день рождения",
                "у меня проблема, нужен оператор",
                "мы опоздаем, надо поменять время брони",
                "хочу посмотреть интерьер",
                "хочу купить сабражную цепь",
                "хотим сабраж с шампанским",
                "хочу оставить отзыв"
        };

        for (String request : productRequests) {
            assertThat(scenario.supports(incoming, BotState.READY_FOR_DIALOG, request))
                    .as(request)
                    .isFalse();
        }
    }

    @Test
    void doesNotStealTableBookingWithTodayWord() {
        IncomingMessage incoming = telegram("хочу столик сегодня на двоих");

        assertThat(scenario.supports(incoming, BotState.READY_FOR_DIALOG, incoming.text())).isFalse();
    }

    @Test
    void safeExitStopsActiveScenario() {
        IncomingMessage incoming = telegram("стоп");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.AUCTION_WAIT_BID, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("Остановил текущий сценарий");
        assertThat(outgoing.actions()).containsExactly("SAFE_EXIT", "OPEN_MAIN_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void ownsOnlyReadyForDialog() {
        assertThat(scenario.owns(BotState.READY_FOR_DIALOG)).isTrue();
        assertThat(scenario.owns(BotState.TIP_CONFIRMATION)).isFalse();
        assertThat(scenario.owns(BotState.AUCTION_WAIT_BID)).isFalse();
        assertThat(scenario.owns(BotState.AI_FALLBACK)).isFalse();
    }

    private IncomingMessage telegram(String text) {
        return IncomingMessage.telegram(
                1773317437L,
                1773317437L,
                356,
                284069928,
                text,
                null,
                "Наталья",
                "Поединенко",
                "Poedinenko",
                "ru",
                false,
                "284069928"
        );
    }
}
