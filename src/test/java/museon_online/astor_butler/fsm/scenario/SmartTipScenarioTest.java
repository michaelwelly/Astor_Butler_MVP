package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.tip.TipOrder;
import museon_online.astor_butler.domain.tip.TipOrderCommand;
import museon_online.astor_butler.domain.tip.TipOrderStatus;
import museon_online.astor_butler.domain.tip.TipService;
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
class SmartTipScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private TipService tipService;

    private SmartTipScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new SmartTipScenario(fsmStorage, tipService);
    }

    @Test
    void asksForAmountWhenTipIntentHasNoMoney() {
        IncomingMessage incoming = telegram("хочу оставить чаевые");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TIP_COLLECT_AMOUNT.name());
        assertThat(outgoing.actions()).containsExactly("SMART_TIP", "ASK_TIP_AMOUNT");
        assertThat(outgoing.text()).contains("Какую сумму");
        verify(fsmStorage).setState(incoming.chatId(), BotState.TIP_COLLECT_AMOUNT);
    }

    @Test
    void movesToConfirmationWhenAmountIsPresent() {
        IncomingMessage incoming = telegram("оставить чаевые 1000 рублей");
        when(tipService.createDraft(any(TipOrderCommand.class))).thenReturn(tipOrder());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TIP_CONFIRMATION.name());
        assertThat(outgoing.actions()).containsExactly("SMART_TIP", "TIP_CONFIRMATION");
        assertThat(outgoing.metadata()).containsEntry("paymentBoundary", "SBP_FUTURE_INTEGRATION");
        assertThat(outgoing.metadata()).containsEntry("tipOrderId", 55L);
        assertThat(outgoing.text()).contains("#55", "Команда AERIS", "1000 ₽");
        verify(fsmStorage).setState(incoming.chatId(), BotState.TIP_CONFIRMATION);
    }

    @Test
    void confirmsTipDraftAndReturnsToMainMenu() {
        IncomingMessage incoming = telegram("да");
        when(tipService.confirmLatestDraft(incoming.chatId())).thenReturn(confirmedTipOrder());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.TIP_CONFIRMATION, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.actions()).containsExactly("SMART_TIP", "TIP_DRAFT_CONFIRMED", "RETURN_MAIN_MENU");
        assertThat(outgoing.text()).contains("благодарность #55", "ожидание оплаты", "1000 ₽");
        assertThat(outgoing.metadata()).containsEntry("tipOrderId", 55L);
        assertThat(outgoing.metadata()).containsEntry("tipOrderStatus", "AWAITING_PAYMENT");
        verify(tipService).confirmLatestDraft(incoming.chatId());
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void cancelsTipDraftAndReturnsToMainMenu() {
        IncomingMessage incoming = telegram("нет");
        when(tipService.cancelLatestDraft(incoming.chatId())).thenReturn(cancelledTipOrder());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.TIP_CONFIRMATION, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.actions()).containsExactly("SMART_TIP", "TIP_CANCELLED", "RETURN_MAIN_MENU");
        assertThat(outgoing.text()).contains("отменил draft чаевых #55");
        assertThat(outgoing.metadata()).containsEntry("tipOrderId", 55L);
        assertThat(outgoing.metadata()).containsEntry("tipOrderStatus", "CANCELLED");
        verify(tipService).cancelLatestDraft(incoming.chatId());
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

    private TipOrder tipOrder() {
        return new TipOrder(
                55L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                1L,
                "Команда AERIS",
                TipOrderStatus.AWAITING_GUEST_CONFIRMATION,
                "TELEGRAM",
                100_000L,
                "RUB",
                "Наталья Поединенко",
                "оставить чаевые 1000 рублей",
                null,
                null,
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }

    private TipOrder confirmedTipOrder() {
        return tipOrder(TipOrderStatus.AWAITING_PAYMENT);
    }

    private TipOrder cancelledTipOrder() {
        return tipOrder(TipOrderStatus.CANCELLED);
    }

    private TipOrder tipOrder(TipOrderStatus status) {
        return new TipOrder(
                55L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                1L,
                "Команда AERIS",
                status,
                "TELEGRAM",
                100_000L,
                "RUB",
                "Наталья Поединенко",
                "оставить чаевые 1000 рублей",
                null,
                null,
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }
}
