package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuietGuideScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private AerisMediaCatalog mediaCatalog;

    private QuietGuideScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new QuietGuideScenario(fsmStorage, mediaCatalog);
        lenient().when(mediaCatalog.interiorTour()).thenReturn(new MediaAsset(
                "AERIS_INTERIOR_TOUR",
                "AERIS",
                "QUIET_GUIDE",
                "VIDEO_TOUR",
                "AERIS interior tour",
                "astor-media",
                "content/aeris/interior/INTERIOR.mp4",
                "INTERIOR.mp4",
                "video/mp4",
                true
        ));
        ReflectionTestUtils.setField(scenario, "interiorVideoAssetCode", "AERIS_INTERIOR_TOUR");
    }

    @Test
    void sendsInteriorVideoMetadata() {
        IncomingMessage incoming = telegram("покажи ресторан внутри");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.metadata()).containsEntry("videoObjectKey", "content/aeris/interior/INTERIOR.mp4");
        assertThat(outgoing.metadata()).containsEntry("videoFilename", "INTERIOR.mp4");
        assertThat(outgoing.metadata()).containsEntry("videoSendMode", "DOCUMENT");
        assertThat(outgoing.actions()).containsExactly("QUIET_GUIDE", "INTERIOR_VIDEO", "QUIET_GUIDE_DELIVERED", "RETURN_MAIN_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void answersConceptWithApprovedCopy() {
        IncomingMessage incoming = telegram("какая у вас концепция?");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.html()).isTrue();
        assertThat(outgoing.text()).contains("Гастрономическая экспедиция", "Георгия Матвеева", "21 страны");
        assertThat(outgoing.metadata()).containsEntry("contentKind", "CONCEPT");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
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
