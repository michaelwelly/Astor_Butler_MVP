package museon_online.astor_butler.api.fsm;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.fsm.FsmRuntimeStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fsm")
@Tag(name = "FSM API", description = "FSM events, state read model and safe reset")
@RequiredArgsConstructor
public class FsmController {

    private final FsmRuntimeStateService runtimeStateService;

    @PostMapping("/events")
    @Operation(summary = "Accept normalized inbound event")
    public ResponseEntity<FsmEventResponse> acceptEvent(@RequestBody FsmEventRequest request) {
        return ResponseEntity.accepted().body(new FsmEventResponse(
                UUID.randomUUID(),
                request.userId(),
                "ACCEPTED",
                "PENDING_ORCHESTRATION",
                Instant.now()
        ));
    }

    @GetMapping("/telegram/{chatId}/state")
    @Operation(summary = "Get live Telegram FSM state from PostgreSQL and Redis")
    public ResponseEntity<FsmRuntimeStateService.TelegramFsmStateView> telegramState(
            @PathVariable("chatId") Long chatId
    ) {
        return ResponseEntity.ok(runtimeStateService.getTelegramState(chatId));
    }

    @PostMapping("/telegram/{chatId}/reset")
    @Operation(summary = "Reset Telegram FSM runtime to first-touch or main-menu state")
    public ResponseEntity<FsmRuntimeStateService.TelegramFsmStateView> resetTelegramState(
            @PathVariable("chatId") Long chatId
    ) {
        return ResponseEntity.accepted().body(runtimeStateService.resetTelegramState(chatId));
    }

    @PutMapping("/telegram/{chatId}/state")
    @Operation(summary = "Replace Telegram FSM state for internal recovery tools")
    public ResponseEntity<FsmRuntimeStateService.TelegramFsmStateView> replaceTelegramState(
            @PathVariable("chatId") Long chatId,
            @RequestBody FsmStateRequest request
    ) {
        return ResponseEntity.ok(runtimeStateService.replaceTelegramState(chatId, request.state()));
    }

    @DeleteMapping("/telegram/{chatId}/state")
    @Operation(summary = "Delete Telegram FSM state, pending intents and booking draft")
    public ResponseEntity<Void> deleteTelegramState(@PathVariable("chatId") Long chatId) {
        runtimeStateService.deleteTelegramState(chatId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/state")
    @Operation(summary = "Get current user FSM state")
    public ResponseEntity<FsmStateResponse> state(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(new FsmStateResponse(userId, "SAFE_IDLE", Map.of(), Instant.now()));
    }

    @PostMapping("/users/{userId}/reset")
    @Operation(summary = "Reset scenario to safe state")
    public ResponseEntity<FsmStateResponse> reset(@PathVariable("userId") UUID userId) {
        return ResponseEntity.accepted().body(new FsmStateResponse(userId, "SAFE_IDLE", Map.of("reset", "true"), Instant.now()));
    }

    @PutMapping("/users/{userId}/state")
    @Operation(summary = "Replace user FSM state for internal recovery tools")
    public ResponseEntity<FsmStateResponse> replaceState(@PathVariable("userId") UUID userId, @RequestBody FsmStateRequest request) {
        return ResponseEntity.ok(new FsmStateResponse(userId, request.state(), request.context(), Instant.now()));
    }

    @DeleteMapping("/users/{userId}/state")
    @Operation(summary = "Delete user FSM state and return to safe idle")
    public ResponseEntity<Void> deleteState(@PathVariable("userId") UUID userId) {
        return ResponseEntity.noContent().build();
    }

    public record FsmEventRequest(UUID userId, String source, String eventType, String idempotencyKey, Map<String, Object> payload) {
    }

    public record FsmEventResponse(UUID eventId, UUID userId, String status, String nextState, Instant acceptedAt) {
    }

    public record FsmStateResponse(UUID userId, String state, Map<String, Object> context, Instant updatedAt) {
    }

    public record FsmStateRequest(String state, Map<String, Object> context) {
    }
}
