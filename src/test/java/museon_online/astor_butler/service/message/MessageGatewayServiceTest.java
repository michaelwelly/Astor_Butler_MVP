package museon_online.astor_butler.service.message;

import museon_online.astor_butler.domain.telegram.TelegramIntakeService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.scenario.FirstTouchScenario;
import museon_online.astor_butler.fsm.scenario.MainMenuScenario;
import museon_online.astor_butler.fsm.scenario.TableBookingScenario;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.kafka.UserEventProducer;
import museon_online.astor_butler.llm.OllamaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageGatewayServiceTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private OllamaClient ollamaClient;

    @Mock
    private TelegramIntakeService telegramIntakeService;

    @Mock
    private FirstTouchScenario firstTouchScenario;

    @Mock
    private MainMenuScenario mainMenuScenario;

    @Mock
    private TableBookingScenario tableBookingScenario;

    @Mock
    private UserEventProducer userEventProducer;

    @Mock
    private LlmScenarioPromptCatalog llmScenarioPromptCatalog;

    private MessageGatewayService service;

    @BeforeEach
    void setUp() {
        service = new MessageGatewayService(
                fsmStorage,
                ollamaClient,
                telegramIntakeService,
                firstTouchScenario,
                mainMenuScenario,
                tableBookingScenario,
                userEventProducer,
                llmScenarioPromptCatalog
        );
        ReflectionTestUtils.setField(service, "adminChatId", "100500");
        ReflectionTestUtils.setField(service, "analyticsChatId", "100501");
        ReflectionTestUtils.setField(service, "logConversationsEnabled", true);
    }

    @Test
    void returnsFallbackWhenLlmTimesOut() {
        IncomingMessage incoming = telegram("Check админки");
        when(fsmStorage.getState(incoming.chatId())).thenReturn(BotState.READY_FOR_DIALOG);
        when(llmScenarioPromptCatalog.tableBookingContract()).thenReturn("table booking contract");
        when(firstTouchScenario.supports(eq(incoming), eq(BotState.READY_FOR_DIALOG), eq("Check админки")))
                .thenReturn(false);
        when(mainMenuScenario.supports(eq(incoming), eq(BotState.READY_FOR_DIALOG), eq("Check админки")))
                .thenReturn(false);
        when(tableBookingScenario.supports(eq(incoming), eq(BotState.READY_FOR_DIALOG), eq("Check админки")))
                .thenReturn(false);
        when(ollamaClient.ask(any())).thenThrow(new ResourceAccessException("Read timed out"));

        OutgoingMessage outgoing = service.handle(incoming);

        assertThat(outgoing.fallback()).isTrue();
        assertThat(outgoing.nextState()).isEqualTo(BotState.AI_FALLBACK.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.adminAlert().chatId()).isEqualTo("100500");
        assertThat(outgoing.adminAlert().text()).contains("Astor Butler / fallback");
        assertThat(outgoing.actions()).containsExactly("FALLBACK", "ADMIN_ALERT");
        verify(fsmStorage).setState(incoming.chatId(), BotState.AI_FALLBACK);
        verify(userEventProducer).publishIncomingMessage(incoming, BotState.READY_FOR_DIALOG, outgoing);
    }

    @Test
    void routesTableBookingIntentBeforeLlm() {
        IncomingMessage incoming = telegram("Хочу забронировать столик завтра на 20:00 на двоих");
        OutgoingMessage scenarioResponse = OutgoingMessage.of(
                incoming,
                "Отправляю план зала AERIS.",
                BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION.name(),
                false,
                false,
                false,
                false,
                AdminAlert.none(),
                java.util.List.of("TABLE_BOOKING_SHOW_PLAN", "SEND_HALL_PLAN")
        ).withMetadata(java.util.Map.of("documentResource", "classpath:booking/aeris-plan.pdf"));

        when(fsmStorage.getState(incoming.chatId())).thenReturn(BotState.READY_FOR_DIALOG);
        when(firstTouchScenario.supports(eq(incoming), eq(BotState.READY_FOR_DIALOG), eq(incoming.text())))
                .thenReturn(false);
        when(mainMenuScenario.supports(eq(incoming), eq(BotState.READY_FOR_DIALOG), eq(incoming.text())))
                .thenReturn(false);
        when(tableBookingScenario.supports(eq(incoming), eq(BotState.READY_FOR_DIALOG), eq(incoming.text())))
                .thenReturn(true);
        when(tableBookingScenario.handle(eq(incoming), eq(BotState.READY_FOR_DIALOG), eq(incoming.text())))
                .thenReturn(scenarioResponse);

        OutgoingMessage outgoing = service.handle(incoming);

        assertThat(outgoing.nextState()).isEqualTo(BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION.name());
        assertThat(outgoing.metadata()).containsEntry("documentResource", "classpath:booking/aeris-plan.pdf");
        verify(ollamaClient, never()).ask(any());
        verify(userEventProducer).publishIncomingMessage(incoming, BotState.READY_FOR_DIALOG, outgoing);
    }

    @Test
    void routesMainMenuProductIntentBeforeLlm() {
        IncomingMessage incoming = telegram("хочу оставить чаевые 1000");
        OutgoingMessage scenarioResponse = OutgoingMessage.of(
                incoming,
                "Принял сумму чаевых.",
                BotState.TIP_CONFIRMATION.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                java.util.List.of("SMART_TIP", "TIP_CONFIRMATION")
        );

        when(fsmStorage.getState(incoming.chatId())).thenReturn(BotState.READY_FOR_DIALOG);
        when(firstTouchScenario.supports(eq(incoming), eq(BotState.READY_FOR_DIALOG), eq(incoming.text())))
                .thenReturn(false);
        when(mainMenuScenario.supports(eq(incoming), eq(BotState.READY_FOR_DIALOG), eq(incoming.text())))
                .thenReturn(true);
        when(mainMenuScenario.handle(eq(incoming), eq(BotState.READY_FOR_DIALOG), eq(incoming.text())))
                .thenReturn(scenarioResponse);

        OutgoingMessage outgoing = service.handle(incoming);

        assertThat(outgoing.nextState()).isEqualTo(BotState.TIP_CONFIRMATION.name());
        assertThat(outgoing.actions()).containsExactly("SMART_TIP", "TIP_CONFIRMATION");
        verify(ollamaClient, never()).ask(any());
        verify(tableBookingScenario, never()).supports(any(), any(), any());
        verify(userEventProducer).publishIncomingMessage(incoming, BotState.READY_FOR_DIALOG, outgoing);
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
