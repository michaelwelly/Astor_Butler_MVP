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
class FeedbackScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    private FeedbackScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new FeedbackScenario(fsmStorage);
        ReflectionTestUtils.setField(scenario, "adminChatId", "100500");
    }

    @Test
    void asksForFeedbackTextWhenGuestOnlyStartsFeedback() {
        IncomingMessage incoming = telegram("оставить отзыв");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.FEEDBACK_COLLECT_TEXT.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly("FEEDBACK", "ASK_FEEDBACK_TEXT");
        verify(fsmStorage).setState(incoming.chatId(), BotState.FEEDBACK_COLLECT_TEXT);
    }

    @Test
    void sendsAdminAlertWhenFeedbackTextWasCollected() {
        IncomingMessage incoming = telegram("Очень понравился сервис и винная карта");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.FEEDBACK_COLLECT_TEXT, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.adminAlert().chatId()).isEqualTo("100500");
        assertThat(outgoing.adminAlert().text()).contains("Astor Butler / feedback", "Наталья Поединенко", "Очень понравился сервис");
        assertThat(outgoing.actions()).containsExactly("FEEDBACK", "FEEDBACK_TEXT_RECEIVED", "ADMIN_ALERT", "RETURN_MAIN_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void sendsDirectFeedbackToAdminChat() {
        IncomingMessage incoming = telegram("отзыв: интерьер красивый, но музыку хотелось тише");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.actions()).containsExactly("FEEDBACK", "FEEDBACK_DIRECT_TEXT", "ADMIN_ALERT", "RETURN_MAIN_MENU");
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
