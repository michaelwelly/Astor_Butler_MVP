package museon_online.astor_butler.fsm.understanding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import museon_online.astor_butler.fsm.core.BotState;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class GuestInputUnderstandingServiceTest {

    private final GuestInputUnderstandingService service = new GuestInputUnderstandingService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void goldenCorpusMapsLiveGuestPhrasesIntoMachineReadableInput() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/understanding/guest-input-golden-corpus.jsonl")),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JsonNode testCase = objectMapper.readTree(line);
                UnderstoodInput understood = service.understand(
                        testCase.get("text").asText(),
                        BotState.valueOf(testCase.get("state").asText())
                );

                assertThat(understood.primaryIntent())
                        .as(testCase.get("text").asText())
                        .isEqualTo(InputIntent.valueOf(testCase.get("intent").asText()));
                if (testCase.has("normalized")) {
                    assertThat(understood.normalizedText()).contains(testCase.get("normalized").asText());
                }
                if (testCase.has("slot")) {
                    String slot = testCase.get("slot").asText();
                    assertThat(understood.slots()).containsKey(slot);
                    if (testCase.has("slotValue")) {
                        assertThat(understood.slots().get(slot).value()).isEqualTo(testCase.get("slotValue").asText());
                    }
                }
            }
        }
    }

    @Test
    void lowConfidenceInputAsksForClarificationInsteadOfPretendingToUnderstand() {
        UnderstoodInput understood = service.understand("онсрантайм цпуид что-то там", BotState.READY_FOR_DIALOG);

        assertThat(understood.primaryIntent()).isEqualTo(InputIntent.UNKNOWN);
        assertThat(understood.needsClarification()).isTrue();
        assertThat(understood.clarificationQuestion()).contains("бронь", "меню");
    }
}
