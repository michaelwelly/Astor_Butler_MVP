package museon_online.astor_butler.fsm.understanding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import museon_online.astor_butler.domain.semantic.EmbeddingProvider;
import museon_online.astor_butler.domain.semantic.IntentExampleRepository;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.model.ModelCapability;
import museon_online.astor_butler.model.ModelGateway;
import museon_online.astor_butler.model.ModelTextRequest;
import museon_online.astor_butler.model.ModelTextResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        UnderstoodInput soloParty = service.understand("На одного", BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE);
        UnderstoodInput wordParty = service.understand("На троих", BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE);
        UnderstoodInput table = service.understand("7", BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION);

        assertThat(time.primaryIntent()).isEqualTo(InputIntent.PROVIDE_TIME);
        assertThat(time.slots().get("time").value()).isEqualTo("20:00");
        assertThat(party.primaryIntent()).isEqualTo(InputIntent.PROVIDE_PARTY_SIZE);
        assertThat(party.slots().get("partySize").value()).isEqualTo("2");
        assertThat(soloParty.primaryIntent()).isEqualTo(InputIntent.PROVIDE_PARTY_SIZE);
        assertThat(soloParty.slots().get("partySize").value()).isEqualTo("1");
        assertThat(wordParty.primaryIntent()).isEqualTo(InputIntent.PROVIDE_PARTY_SIZE);
        assertThat(wordParty.slots().get("partySize").value()).isEqualTo("3");
        assertThat(wordParty.normalizedText()).contains("3 гостей");
        assertThat(table.primaryIntent()).isEqualTo(InputIntent.PROVIDE_TABLE_SELECTION);
        assertThat(table.slots().get("tableNumber").value()).isEqualTo("7");
    }

    @Test
    void vagueEveningWishDoesNotStartSideEffectingTableBooking() {
        UnderstoodInput understood = service.understand("хочу красиво вечером но не знаю", BotState.READY_FOR_DIALOG);

        assertThat(understood.primaryIntent()).isNotEqualTo(InputIntent.TABLE_BOOKING);
        assertThat(understood.needsClarification()).isTrue();
    }

    @Test
    void restaurantStoryRoutesToQuietGuide() {
        UnderstoodInput understood = service.understand("Расскажи про ресторан", BotState.READY_FOR_DIALOG);

        assertThat(understood.primaryIntent()).isEqualTo(InputIntent.QUIET_GUIDE);
        assertThat(understood.needsClarification()).isFalse();
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

    @Test
    void semanticIntentLookupIsBestEffortWhenEmbeddingsAreUnavailable() {
        IntentExampleRepository repository = mock(IntentExampleRepository.class);
        EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);
        when(repository.findBestLexicalMatch(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(embeddingProvider.embed(anyString())).thenThrow(new ResourceAccessException("llm-gateway"));

        GuestInputUnderstandingService service = new GuestInputUnderstandingService(
                repository,
                embeddingProvider,
                List.of()
        );

        assertThatCode(() -> service.understand("какая-то непонятная просьба", BotState.READY_FOR_DIALOG))
                .doesNotThrowAnyException();

        UnderstoodInput unclear = service.understand("какая-то непонятная просьба", BotState.READY_FOR_DIALOG);
        assertThat(unclear.primaryIntent()).isEqualTo(InputIntent.UNKNOWN);
    }

    @Test
    void tableSelectionNumberWithWordIsNotNormalizedAsTime() {
        UnderstoodInput understood = service.understand("10 стол", BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION);

        assertThat(understood.primaryIntent()).isEqualTo(InputIntent.PROVIDE_TABLE_SELECTION);
        assertThat(understood.slots()).containsKey("tableNumber");
        assertThat(understood.slots().get("tableNumber").value()).isEqualTo("10");
        assertThat(understood.slots()).doesNotContainKey("time");
    }

    @Test
    void autoTableSelectionPhraseIsHandledLexically() {
        UnderstoodInput understood = service.understand("подбери сам", BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION);

        assertThat(understood.primaryIntent()).isEqualTo(InputIntent.PROVIDE_TABLE_SELECTION);
        assertThat(understood.slots()).containsKey("seatingPreference");
        assertThat(understood.confidence()).isGreaterThanOrEqualTo(0.72);
    }

    @Test
    void llmUnderstandingCanSupplyIntentAndSlotsBehindFeatureFlag() {
        ModelGateway modelGateway = mock(ModelGateway.class);
        when(modelGateway.generateText(any(ModelTextRequest.class))).thenReturn(new ModelTextResponse(
                """
                {
                  "intent": "TABLE_BOOKING",
                  "confidence": 0.91,
                  "slots": {
                    "date": "завтра",
                    "time": "20:00",
                    "partySize": "4",
                    "seatingPreference": "спокойное место"
                  },
                  "missingSlots": [],
                  "replyDraft": "Понял, завтра после восьми на четверых, место поспокойнее."
                }
                """,
                "test-llm",
                "structured-test",
                ModelCapability.TEXT_GENERATION,
                Duration.ofMillis(12),
                false,
                Map.of()
        ));
        LlmUnderstandingService llm = new LlmUnderstandingService(modelGateway, objectMapper);
        ReflectionTestUtils.setField(llm, "enabled", true);
        ReflectionTestUtils.setField(llm, "minConfidence", 0.70);
        GuestInputUnderstandingService withLlm = new GuestInputUnderstandingService(
                null,
                null,
                List.of(),
                llm
        );

        UnderstoodInput understood = withLlm.understand(
                "мы завтра после восьми будем где-то вчетвером, можно место поспокойнее?",
                BotState.READY_FOR_DIALOG
        );

        assertThat(understood.primaryIntent()).isEqualTo(InputIntent.TABLE_BOOKING);
        assertThat(understood.slots()).containsKeys("date", "time", "partySize", "seatingPreference");
        assertThat(understood.slots().get("time").value()).isEqualTo("20:00");
        assertThat(understood.slots().get("partySize").value()).isEqualTo("4");
        assertThat(understood.slots().get("seatingPreference").value()).contains("спокойное");
    }

    @Test
    void llmUnderstandingTimeoutFallsBackToLocalPipeline() {
        ModelGateway modelGateway = mock(ModelGateway.class);
        when(modelGateway.generateText(any(ModelTextRequest.class))).thenAnswer(invocation -> {
            Thread.sleep(200);
            return ModelTextResponse.text("", "slow-test", "slow-model", Duration.ofMillis(200));
        });
        LlmUnderstandingService llm = new LlmUnderstandingService(modelGateway, objectMapper);
        ReflectionTestUtils.setField(llm, "enabled", true);
        ReflectionTestUtils.setField(llm, "timeoutMs", 30L);
        GuestInputUnderstandingService withLlm = new GuestInputUnderstandingService(
                null,
                null,
                List.of(),
                llm
        );

        UnderstoodInput understood = withLlm.understand(
                "Хочу стол завтра в 20:00 на двоих у окна",
                BotState.READY_FOR_DIALOG
        );

        assertThat(understood.primaryIntent()).isEqualTo(InputIntent.TABLE_BOOKING);
        assertThat(understood.slots()).containsKeys("date", "time", "partySize", "seatingPreference");
    }

    @Test
    void tableBookingTargetCorpusDocumentsCurrentBaselineAndKnownGaps() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/understanding/table-booking-target-corpus.jsonl")),
                StandardCharsets.UTF_8
        ))) {
            String line;
            int passCases = 0;
            int knownGaps = 0;
            while ((line = reader.readLine()) != null) {
                JsonNode testCase = objectMapper.readTree(line);
                String status = testCase.path("status").asText();
                UnderstoodInput understood = service.understand(
                        testCase.get("text").asText(),
                        BotState.valueOf(testCase.get("state").asText())
                );
                if ("KNOWN_GAP".equals(status)) {
                    knownGaps++;
                    continue;
                }
                passCases++;
                assertThat(understood.primaryIntent())
                        .as(testCase.path("id").asText() + " " + testCase.path("text").asText())
                        .isEqualTo(InputIntent.valueOf(testCase.get("intent").asText()));
                JsonNode slots = testCase.path("slots");
                slots.fields().forEachRemaining(entry -> assertThat(understood.slots())
                        .as(testCase.path("id").asText() + " slot " + entry.getKey())
                        .containsKey(entry.getKey()));
            }

            assertThat(passCases).isGreaterThanOrEqualTo(10);
            assertThat(knownGaps).isGreaterThanOrEqualTo(10);
        }
    }
}
