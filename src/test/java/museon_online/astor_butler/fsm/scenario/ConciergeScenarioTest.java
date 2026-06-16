package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.concierge.ConciergeRequest;
import museon_online.astor_butler.domain.concierge.ConciergeRequestCommand;
import museon_online.astor_butler.domain.concierge.ConciergeRequestService;
import museon_online.astor_butler.domain.concierge.ConciergeRequestStatus;
import museon_online.astor_butler.domain.concierge.ConciergeRequestType;
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
class ConciergeScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private ConciergeRequestService conciergeRequestService;

    private ConciergeScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ConciergeScenario(fsmStorage, conciergeRequestService);
        ReflectionTestUtils.setField(scenario, "adminChatId", "100500");
    }

    @Test
    void asksForRequestWhenGuestOnlyCallsConcierge() {
        IncomingMessage incoming = telegram("консьерж");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.CONCIERGE_COLLECT_REQUEST.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly("CONCIERGE", "ASK_CONCIERGE_REQUEST");
        verify(fsmStorage).setState(incoming.chatId(), BotState.CONCIERGE_COLLECT_REQUEST);
    }

    @Test
    void sendsCollectedConciergeRequestToAdmin() {
        IncomingMessage incoming = telegram("подготовьте свечу к десерту");
        when(conciergeRequestService.classify(incoming.text())).thenReturn(ConciergeRequestType.CELEBRATION);
        when(conciergeRequestService.createRequest(any(ConciergeRequestCommand.class))).thenReturn(request());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.CONCIERGE_COLLECT_REQUEST, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("сервисную заявку #66");
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.adminAlert().chatId()).isEqualTo("100500");
        assertThat(outgoing.adminAlert().text()).contains("Astor Butler / concierge", "подготовьте свечу");
        assertThat(outgoing.actions()).containsExactly("CONCIERGE", "CONCIERGE_DETAILS_RECEIVED", "ADMIN_ALERT", "RETURN_MAIN_MENU");
        assertThat(outgoing.metadata()).containsEntry("conciergeRequestId", 66L);
        assertThat(outgoing.metadata()).containsEntry("requestType", "CELEBRATION");
        assertThat(outgoing.metadata()).containsEntry("handoffBoundary", "TEAM_CONFIRMATION_REQUIRED");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void sendsDirectConciergeRequestToAdmin() {
        IncomingMessage incoming = telegram("передай команде, принесите плед на веранду");
        when(conciergeRequestService.classify("принесите плед на веранду")).thenReturn(ConciergeRequestType.COMFORT);
        when(conciergeRequestService.createRequest(any(ConciergeRequestCommand.class))).thenReturn(request());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.actions()).containsExactly("CONCIERGE", "CONCIERGE_DIRECT_REQUEST", "ADMIN_ALERT", "RETURN_MAIN_MENU");
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

    private ConciergeRequest request() {
        return new ConciergeRequest(
                66L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                ConciergeRequestType.CELEBRATION,
                ConciergeRequestStatus.PENDING_TEAM,
                "TELEGRAM",
                "Наталья Поединенко",
                "подготовьте свечу к десерту",
                "100500",
                BotState.CONCIERGE_COLLECT_REQUEST.name(),
                "284069875",
                "{}",
                Instant.parse("2026-06-16T10:00:00Z"),
                Instant.parse("2026-06-16T10:00:00Z")
        );
    }
}
