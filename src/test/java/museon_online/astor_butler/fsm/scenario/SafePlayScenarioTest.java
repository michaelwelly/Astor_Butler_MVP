package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.booking.TableReservationRepository;
import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
import museon_online.astor_butler.domain.semantic.SemanticRetrievalService;
import museon_online.astor_butler.domain.semantic.SemanticSearchResult;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.reply.ScenarioReply;
import museon_online.astor_butler.fsm.reply.ScenarioReplyComposer;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafePlayScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private TableReservationRepository tableReservationRepository;

    @Mock
    private AerisMediaCatalog mediaCatalog;

    @Mock
    private SemanticRetrievalService semanticRetrievalService;

    @Mock
    private ScenarioReplyComposer replyComposer;

    private SafePlayScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new SafePlayScenario(fsmStorage, tableReservationRepository, mediaCatalog, semanticRetrievalService, replyComposer);
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
        when(tableReservationRepository.findActiveOrdersByChatId(incoming.chatId())).thenReturn(java.util.List.of());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.adminAlert().text()).contains("Astor Butler / safe play", "Не давать гостю dangerous how-to", "Активная бронь");
        assertThat(outgoing.adminAlert().buttons()).hasSize(1);
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
        when(tableReservationRepository.findActiveOrdersByChatId(incoming.chatId())).thenReturn(java.util.List.of());

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

    @Test
    void answersSabrageWineAdviceWithRagAndWineMenuAsset() {
        IncomingMessage incoming = telegram("какое игристое взять под сабраж");
        MediaAsset wineMenu = new MediaAsset(
                "AERIS_MENU_WINE",
                "AERIS",
                "QUIET_GUIDE",
                "PDF_MENU",
                "Винная карта",
                "astor-media",
                "content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf",
                "WINE MENU 2026 FINAL.pdf",
                "application/pdf",
                true
        );
        when(mediaCatalog.wineMenu()).thenReturn(wineMenu);
        when(semanticRetrievalService.search(eq("AERIS"), anyString(), anyList(), eq(5))).thenReturn(List.of(
                new SemanticSearchResult(
                        UUID.randomUUID(),
                        "AERIS_MENU_WINE_SOURCE",
                        "SEMANTIC_SEED",
                        "Игристое бутылками из винной карты",
                        "Mont Marcal Cava Brut — 5 500; Tenuta Dodici 12 Prosecco — 3 500",
                        0.86
                )
        ));
        when(replyComposer.compose(any())).thenReturn(ScenarioReply.fallback("Под сабраж подойдут Cava, Prosecco или Champagne. Команда подтвердит наличие."));

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.text()).contains("Под сабраж");
        assertThat(outgoing.actions()).containsExactly(
                "SAFE_PLAY",
                "SABRAGE_WINE_ADVICE",
                "RAG_CONTEXT_USED",
                "NO_DANGEROUS_HOW_TO",
                "RETURN_MAIN_MENU"
        );
        assertThat(outgoing.metadata()).containsEntry("documentAssetCode", "AERIS_MENU_WINE");
        assertThat(outgoing.metadata()).containsEntry("safetyBoundary", "NO_DANGEROUS_HOW_TO");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void doesNotCaptureSabrageChainPurchaseIntent() {
        IncomingMessage incoming = telegram("хочу купить сабражную цепь");

        assertThat(scenario.supports(incoming, BotState.READY_FOR_DIALOG, incoming.text())).isFalse();
    }

    @Test
    void stillCapturesSabrageRitualIntent() {
        IncomingMessage incoming = telegram("хочу сабражный ритуал к столу");

        assertThat(scenario.supports(incoming, BotState.READY_FOR_DIALOG, incoming.text())).isTrue();
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
