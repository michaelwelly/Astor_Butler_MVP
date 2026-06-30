package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
import museon_online.astor_butler.domain.semantic.SemanticRetrievalService;
import museon_online.astor_butler.domain.semantic.SemanticSearchResult;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.reply.ScenarioReplyComposer;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.model.ModelGateway;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class MenuAssetsScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private AerisMediaCatalog mediaCatalog;

    @Mock
    private SemanticRetrievalService semanticRetrievalService;

    @Mock
    private ModelGateway modelGateway;

    private MenuAssetsScenario scenario;

    @BeforeEach
    void setUp() {
        MediaAsset kitchen = asset(
                "AERIS_MENU_KITCHEN",
                "Кухня / основное меню",
                "content/aeris/menu/kitchen/MENU_AERIS_A4_2026_DIGITAL.pdf",
                "MENU AERIS A4 2026 DIGITAL.pdf"
        );
        MediaAsset bar = asset(
                "AERIS_MENU_BAR",
                "Барная карта",
                "content/aeris/menu/bar/BAR_CARD.pdf",
                "BAR CARD.pdf"
        );
        MediaAsset elements = asset(
                "AERIS_MENU_ELEMENTS",
                "Коктейли / Elements",
                "content/aeris/menu/elements/ELEMENTS_CARD.pdf",
                "ELEMENTS CARD.pdf"
        );
        MediaAsset wine = asset(
                "AERIS_MENU_WINE",
                "Винная карта",
                "content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf",
                "WINE MENU 2026 FINAL.pdf"
        );
        lenient().when(mediaCatalog.kitchenMenu()).thenReturn(kitchen);
        lenient().when(mediaCatalog.barMenu()).thenReturn(bar);
        lenient().when(mediaCatalog.elementsMenu()).thenReturn(elements);
        lenient().when(mediaCatalog.wineMenu()).thenReturn(wine);
        lenient().when(mediaCatalog.allMenus()).thenReturn(List.of(kitchen, bar, elements, wine));
        lenient().when(mediaCatalog.menuRagSource()).thenReturn("media_assets:AERIS_MENU_*");
        lenient().when(semanticRetrievalService.sourceCodesForAssets(List.of(
                "AERIS_MENU_KITCHEN",
                "AERIS_MENU_BAR",
                "AERIS_MENU_ELEMENTS",
                "AERIS_MENU_WINE"
        ))).thenReturn(List.of(
                "AERIS_MENU_KITCHEN_SOURCE",
                "AERIS_MENU_BAR_SOURCE",
                "AERIS_MENU_ELEMENTS_SOURCE",
                "AERIS_MENU_WINE_SOURCE"
        ));
        lenient().when(semanticRetrievalService.sourceCodesForAssets(List.of("AERIS_MENU_WINE")))
                .thenReturn(List.of("AERIS_MENU_WINE_SOURCE"));
        lenient().when(semanticRetrievalService.search(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn(List.of());
        scenario = new MenuAssetsScenario(
                fsmStorage,
                mediaCatalog,
                semanticRetrievalService,
                new ScenarioReplyComposer(modelGateway, false, 900)
        );
    }

    @Test
    void sendsAllMenuDocumentsForGenericMenuRequest() {
        IncomingMessage incoming = telegram("скинь меню");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.actions()).containsExactly("MENU_ASSETS", "MENU_ASSETS_DELIVERED", "RETURN_MAIN_MENU");
        assertThat(documents(outgoing)).extracting(document -> document.get("filename"))
                .containsExactly(
                        "MENU AERIS A4 2026 DIGITAL.pdf",
                        "BAR CARD.pdf",
                        "ELEMENTS CARD.pdf",
                        "WINE MENU 2026 FINAL.pdf"
                );
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void sendsWineOnlyForWineRequest() {
        IncomingMessage incoming = telegram("что у вас по вину?");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(documents(outgoing)).singleElement()
                .satisfies(document -> {
                    assertThat(document).containsEntry("filename", "WINE MENU 2026 FINAL.pdf");
                    assertThat(document).containsEntry("objectKey", "content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf");
                });
        assertThat(outgoing.text()).contains("Винная карта");
    }

    @Test
    void sendsKitchenAndWineForMenuAndWineRequest() {
        IncomingMessage incoming = telegram("покажи меню и винную карту");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(documents(outgoing)).extracting(document -> document.get("filename"))
                .containsExactly(
                        "MENU AERIS A4 2026 DIGITAL.pdf",
                        "WINE MENU 2026 FINAL.pdf"
                );
    }

    @Test
    void includesRagContextForSpecificMenuQuestion() {
        IncomingMessage incoming = telegram("какое вино подойдет к рыбе?");
        when(semanticRetrievalService.search(
                "AERIS",
                incoming.text(),
                List.of("AERIS_MENU_WINE_SOURCE"),
                3
        )).thenReturn(List.of(new SemanticSearchResult(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "AERIS_MENU_WINE_SOURCE",
                "MENU_PDF",
                "Винная карта",
                "Винная карта используется для запросов про wine pairing.",
                0.81
        )));

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.text()).contains("подобрал материалы по смыслу запроса");
        assertThat(outgoing.metadata().get("ragContext")).asList().hasSize(1);
    }

    @Test
    void clarifiesUnknownContinuation() {
        IncomingMessage incoming = telegram("не знаю");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.MENU_ASSETS_CLARIFY, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.MENU_ASSETS_CLARIFY.name());
        assertThat(outgoing.text()).contains("кухню, бар, коктейли, вино или все меню");
        verify(fsmStorage).setState(incoming.chatId(), BotState.MENU_ASSETS_CLARIFY);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> documents(OutgoingMessage outgoing) {
        return (List<Map<String, String>>) outgoing.metadata().get("documents");
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

    private MediaAsset asset(String assetCode, String title, String objectKey, String filename) {
        return new MediaAsset(
                assetCode,
                "AERIS",
                "QUIET_GUIDE",
                "PDF_MENU",
                title,
                "astor-media",
                objectKey,
                filename,
                "application/pdf",
                true
        );
    }
}
