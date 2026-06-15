package museon_online.astor_butler.api.fsm;

import museon_online.astor_butler.domain.fsm.FsmRuntimeStateService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FsmControllerTest {

    private final FsmRuntimeStateService runtimeStateService = mock(FsmRuntimeStateService.class);
    private final FsmController controller = new FsmController(runtimeStateService);

    @Test
    void returnsLiveTelegramState() {
        FsmRuntimeStateService.TelegramFsmStateView view = state("READY_FOR_DIALOG");
        when(runtimeStateService.getTelegramState(1773317437L)).thenReturn(view);

        ResponseEntity<FsmRuntimeStateService.TelegramFsmStateView> response =
                controller.telegramState(1773317437L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(view);
        verify(runtimeStateService).getTelegramState(1773317437L);
    }

    @Test
    void resetsLiveTelegramState() {
        FsmRuntimeStateService.TelegramFsmStateView view = state("READY_FOR_DIALOG");
        when(runtimeStateService.resetTelegramState(1773317437L)).thenReturn(view);

        ResponseEntity<FsmRuntimeStateService.TelegramFsmStateView> response =
                controller.resetTelegramState(1773317437L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(view);
        verify(runtimeStateService).resetTelegramState(1773317437L);
    }

    @Test
    void replacesLiveTelegramState() {
        FsmRuntimeStateService.TelegramFsmStateView view = state("TABLE_BOOKING_SHOW_PLAN");
        when(runtimeStateService.replaceTelegramState(1773317437L, "TABLE_BOOKING_SHOW_PLAN")).thenReturn(view);

        ResponseEntity<FsmRuntimeStateService.TelegramFsmStateView> response =
                controller.replaceTelegramState(1773317437L, new FsmController.FsmStateRequest(
                        "TABLE_BOOKING_SHOW_PLAN",
                        Map.of()
                ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(view);
        verify(runtimeStateService).replaceTelegramState(1773317437L, "TABLE_BOOKING_SHOW_PLAN");
    }

    @Test
    void deletesLiveTelegramState() {
        ResponseEntity<Void> response = controller.deleteTelegramState(1773317437L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(runtimeStateService).deleteTelegramState(1773317437L);
    }

    private FsmRuntimeStateService.TelegramFsmStateView state(String state) {
        return new FsmRuntimeStateService.TelegramFsmStateView(
                1773317437L,
                1773317437L,
                1L,
                "Poedinenko",
                "Наталья",
                "Поединенко",
                "Наталья Поединенко",
                state,
                List.of(),
                true,
                false,
                12L,
                Instant.parse("2026-06-15T08:00:00Z"),
                Instant.parse("2026-06-15T08:01:00Z"),
                Map.of("stateStorage", "redis")
        );
    }
}
