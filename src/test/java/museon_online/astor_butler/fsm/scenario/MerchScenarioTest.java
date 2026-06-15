package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.merch.MerchOrder;
import museon_online.astor_butler.domain.merch.MerchOrderCommand;
import museon_online.astor_butler.domain.merch.MerchOrderStatus;
import museon_online.astor_butler.domain.merch.MerchService;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private MerchService merchService;

    private MerchScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new MerchScenario(fsmStorage, merchService);
        ReflectionTestUtils.setField(scenario, "adminChatId", "100500");
    }

    @Test
    void asksForDetailsWhenGuestOnlyMentionsMerch() {
        IncomingMessage incoming = telegram("мерч");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.MERCH_COLLECT_REQUEST.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly("MERCH", "ASK_MERCH_DETAILS");
        verify(fsmStorage).setState(incoming.chatId(), BotState.MERCH_COLLECT_REQUEST);
    }

    @Test
    void sendsAdminAlertWhenDetailsWereCollected() {
        IncomingMessage incoming = telegram("хочу сабражную цепь в подарок");
        when(merchService.createOrder(any(MerchOrderCommand.class))).thenReturn(merchOrder());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.MERCH_COLLECT_REQUEST, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("заявку по мерчу #99");
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.adminAlert().text()).contains("Astor Butler / merch", "сабражную цепь");
        assertThat(outgoing.actions()).containsExactly("MERCH", "MERCH_DETAILS_RECEIVED", "ADMIN_ALERT", "RETURN_MAIN_MENU");
        assertThat(outgoing.metadata()).containsEntry("merchOrderId", 99L);
        assertThat(outgoing.metadata()).containsEntry("itemId", 7L);
        assertThat(outgoing.metadata()).containsEntry("itemTitle", "Сабражная цепь AERIS");
        assertThat(outgoing.metadata()).containsEntry("orderBoundary", "MANUAL_CONFIRMATION_REQUIRED");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void sendsDirectMerchRequestToAdminChat() {
        IncomingMessage incoming = telegram("хочу купить сабражную цепь");
        when(merchService.createOrder(any(MerchOrderCommand.class))).thenReturn(merchOrder());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.actions()).containsExactly("MERCH", "MERCH_DIRECT_REQUEST", "ADMIN_ALERT", "RETURN_MAIN_MENU");
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

    private MerchOrder merchOrder() {
        return new MerchOrder(
                99L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                7L,
                "Сабражная цепь AERIS",
                MerchOrderStatus.PENDING_TEAM,
                "TELEGRAM",
                1,
                null,
                "RUB",
                "Наталья Поединенко",
                "хочу сабражную цепь в подарок",
                "TEAM_CONFIRMATION_REQUIRED",
                null,
                "{}",
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }
}
