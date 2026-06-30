package museon_online.astor_butler.fsm.reply;

import museon_online.astor_butler.domain.semantic.SemanticSearchResult;
import museon_online.astor_butler.model.ModelCapability;
import museon_online.astor_butler.model.ModelGateway;
import museon_online.astor_butler.model.ModelTextRequest;
import museon_online.astor_butler.model.ModelTextResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScenarioReplyComposerTest {

    @Test
    void returnsFallbackWithoutCallingModelWhenDisabled() {
        ModelGateway modelGateway = mock(ModelGateway.class);
        ScenarioReplyComposer composer = new ScenarioReplyComposer(modelGateway, false, 900);

        ScenarioReply reply = composer.compose(draft("Одобренный ответ."));

        assertThat(reply.text()).isEqualTo("Одобренный ответ.");
        assertThat(reply.generated()).isFalse();
        assertThat(reply.fallbackUsed()).isTrue();
        verify(modelGateway, never()).generateText(any());
    }

    @Test
    void usesGeneratedReplyWhenEnabledAndModelReturnsShortText() {
        ModelGateway modelGateway = mock(ModelGateway.class);
        when(modelGateway.generateText(any(ModelTextRequest.class))).thenReturn(new ModelTextResponse(
                "Конечно. Отправляю карты AERIS и помогу выбрать стол, если захотите.",
                "test-provider",
                "test-model",
                ModelCapability.TEXT_GENERATION,
                Duration.ofMillis(12),
                false,
                Map.of()
        ));
        ScenarioReplyComposer composer = new ScenarioReplyComposer(modelGateway, true, 900);

        ScenarioReply reply = composer.compose(draft("Одобренный ответ."));

        assertThat(reply.text()).contains("Отправляю карты AERIS");
        assertThat(reply.generated()).isTrue();
        assertThat(reply.provider()).isEqualTo("test-provider");
        assertThat(reply.model()).isEqualTo("test-model");
    }

    @Test
    void fallsBackWhenModelFails() {
        ModelGateway modelGateway = mock(ModelGateway.class);
        when(modelGateway.generateText(any(ModelTextRequest.class))).thenThrow(new RuntimeException("timeout"));
        ScenarioReplyComposer composer = new ScenarioReplyComposer(modelGateway, true, 900);

        ScenarioReply reply = composer.compose(draft("Одобренный ответ."));

        assertThat(reply.text()).isEqualTo("Одобренный ответ.");
        assertThat(reply.generated()).isFalse();
        assertThat(reply.fallbackUsed()).isTrue();
    }

    @Test
    void fallsBackWhenModelIsTooSlow() {
        ModelGateway modelGateway = mock(ModelGateway.class);
        when(modelGateway.generateText(any(ModelTextRequest.class))).thenAnswer(invocation -> {
            Thread.sleep(250);
            return new ModelTextResponse(
                    "Поздний ответ модели.",
                    "test-provider",
                    "test-model",
                    ModelCapability.TEXT_GENERATION,
                    Duration.ofMillis(250),
                    false,
                    Map.of()
            );
        });
        ScenarioReplyComposer composer = new ScenarioReplyComposer(modelGateway, true, 900, 50);

        ScenarioReply reply = composer.compose(draft("Одобренный ответ."));

        assertThat(reply.text()).isEqualTo("Одобренный ответ.");
        assertThat(reply.generated()).isFalse();
        assertThat(reply.fallbackUsed()).isTrue();
    }

    private ScenarioReplyDraft draft(String fallback) {
        return ScenarioReplyDraft.of(
                "MENU_ASSETS",
                "READY_FOR_DIALOG",
                "MENU_ASSETS_DELIVERED",
                "покажи меню",
                fallback,
                List.of(new SemanticSearchResult(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "AERIS_MENU_SOURCE",
                        "MENU_PDF",
                        "Меню AERIS",
                        "В меню AERIS есть кухня, бар, коктейли Elements и винная карта.",
                        0.9
                ))
        );
    }
}
