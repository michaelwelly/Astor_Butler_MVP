package museon_online.astor_butler.api.preference;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.preference.GuestPreference;
import museon_online.astor_butler.domain.preference.GuestPreferenceCategory;
import museon_online.astor_butler.domain.preference.GuestPreferenceCommand;
import museon_online.astor_butler.domain.preference.GuestPreferenceService;
import museon_online.astor_butler.domain.preference.GuestPreferenceStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/preferences")
@Tag(name = "Guest Preference API", description = "Durable guest preference memory for personalization")
@RequiredArgsConstructor
public class GuestPreferenceController {

    private final GuestPreferenceService preferenceService;

    @PostMapping
    @Operation(summary = "Create guest preference")
    public ResponseEntity<GuestPreferenceResponse> createPreference(@RequestBody GuestPreferenceCreateRequest request) {
        GuestPreference preference = preferenceService.createPreference(request.toCommand(preferenceService));
        return ResponseEntity.status(HttpStatus.CREATED).body(GuestPreferenceResponse.from(preference));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get guest preference")
    public ResponseEntity<GuestPreferenceResponse> getPreference(@PathVariable("id") Long id) {
        return ResponseEntity.ok(GuestPreferenceResponse.from(preferenceService.getPreference(id)));
    }

    @GetMapping("/telegram/{chatId}")
    @Operation(summary = "List active guest preferences for Telegram chat")
    public ResponseEntity<List<GuestPreferenceResponse>> listTelegramPreferences(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(preferenceService.listActiveByChatId(chatId, limit).stream()
                .map(GuestPreferenceResponse::from)
                .toList());
    }

    public record GuestPreferenceCreateRequest(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            GuestPreferenceCategory category,
            String preferenceText,
            String capturedFromState,
            String correlationId,
            String metadataJson
    ) {
        GuestPreferenceCommand toCommand(GuestPreferenceService service) {
            GuestPreferenceCategory resolvedCategory = category == null
                    ? service.classify(preferenceText)
                    : category;
            return new GuestPreferenceCommand(
                    chatId,
                    telegramUserId,
                    userId,
                    venueCode,
                    resolvedCategory,
                    preferenceText,
                    capturedFromState,
                    correlationId,
                    metadataJson
            );
        }
    }

    public record GuestPreferenceResponse(
            Long id,
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            GuestPreferenceCategory category,
            String preferenceText,
            String source,
            GuestPreferenceStatus status,
            Double confidence,
            String capturedFromState,
            String correlationId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static GuestPreferenceResponse from(GuestPreference preference) {
            return new GuestPreferenceResponse(
                    preference.id(),
                    preference.chatId(),
                    preference.telegramUserId(),
                    preference.userId(),
                    preference.venueCode(),
                    preference.category(),
                    preference.preferenceText(),
                    preference.source(),
                    preference.status(),
                    preference.confidence(),
                    preference.capturedFromState(),
                    preference.correlationId(),
                    preference.createdAt(),
                    preference.updatedAt()
            );
        }
    }
}
