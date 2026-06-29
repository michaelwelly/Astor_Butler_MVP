package museon_online.astor_butler.fsm.understanding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import museon_online.astor_butler.fsm.core.BotState;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    @Test
    void tableBookingBeatsWineMenuWhenGuestMentionsSeatingPreference() {
        UnderstoodInput understood = service.understand(
                "Хочу стол завтра в восемь вечера на двоих, тихий стол в винной комнате",
                BotState.READY_FOR_DIALOG
        );

        assertThat(understood.primaryIntent()).isEqualTo(InputIntent.TABLE_BOOKING);
        assertThat(understood.slots()).containsKeys("date", "time", "partySize", "seatingPreference");
        assertThat(understood.slots().get("time").value()).isEqualTo("20:00");
        assertThat(understood.slots().get("partySize").value()).isEqualTo("2");
    }

    @Test
    void shortNumericRepliesUseCurrentTableBookingState() {
        UnderstoodInput time = service.understand("8", BotState.TABLE_BOOKING_COLLECT_TIME);
        UnderstoodInput party = service.understand("На 2", BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE);
        UnderstoodInput wordParty = service.understand("На троих", BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE);
        UnderstoodInput table = service.understand("7", BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION);

        assertThat(time.primaryIntent()).isEqualTo(InputIntent.PROVIDE_TIME);
        assertThat(time.slots().get("time").value()).isEqualTo("20:00");
        assertThat(party.primaryIntent()).isEqualTo(InputIntent.PROVIDE_PARTY_SIZE);
        assertThat(party.slots().get("partySize").value()).isEqualTo("2");
        assertThat(wordParty.primaryIntent()).isEqualTo(InputIntent.PROVIDE_PARTY_SIZE);
        assertThat(wordParty.slots().get("partySize").value()).isEqualTo("3");
        assertThat(wordParty.normalizedText()).contains("3 гостей");
        assertThat(table.primaryIntent()).isEqualTo(InputIntent.PROVIDE_TABLE_SELECTION);
        assertThat(table.slots().get("tableNumber").value()).isEqualTo("7");
    }

    @Test
    void seatingPreferenceRepliesUseCurrentTableBookingState() {
        UnderstoodInput noPreference = service.understand("нет", BotState.TABLE_BOOKING_COLLECT_SEATING_PREFERENCE);
        UnderstoodInput quietTable = service.understand("тихий стол не у прохода", BotState.TABLE_BOOKING_COLLECT_SEATING_PREFERENCE);

        assertThat(noPreference.primaryIntent()).isEqualTo(InputIntent.PROVIDE_SEATING_PREFERENCE);
        assertThat(noPreference.needsClarification()).isFalse();
        assertThat(quietTable.primaryIntent()).isEqualTo(InputIntent.PROVIDE_SEATING_PREFERENCE);
        assertThat(quietTable.slots()).containsKey("seatingPreference");
    }

    @Test
    void externalNluAdaptersCanEnrichSlotsWithoutOwningFsmRouting() {
        GuestInputUnderstandingService withAdapter = new GuestInputUnderstandingService(
                null,
                null,
                List.of((text, currentState) -> new RussianNluResult(
                        "test",
                        List.of(new RussianNluSlot("time", "20:00", 0.91, "test"))
                ))
        );

        UnderstoodInput understood = withAdapter.understand("поставь бронь вечером", BotState.TABLE_BOOKING_COLLECT_TIME);

        assertThat(understood.primaryIntent()).isEqualTo(InputIntent.PROVIDE_TIME);
        assertThat(understood.slots().get("time").value()).isEqualTo("20:00");
        assertThat(understood.normalizedText()).contains("20:00");
    }
}
