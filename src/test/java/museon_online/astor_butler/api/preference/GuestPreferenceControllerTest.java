package museon_online.astor_butler.api.preference;

import museon_online.astor_butler.domain.preference.GuestPreference;
import museon_online.astor_butler.domain.preference.GuestPreferenceCategory;
import museon_online.astor_butler.domain.preference.GuestPreferenceService;
import museon_online.astor_butler.domain.preference.GuestPreferenceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuestPreferenceControllerTest {

    private final GuestPreferenceService preferenceService = mock(GuestPreferenceService.class);
    private final GuestPreferenceController controller = new GuestPreferenceController(preferenceService);

    @Test
    void listsActiveTelegramPreferences() {
        when(preferenceService.listActiveByChatId(1773317437L, 5)).thenReturn(List.of(preference(GuestPreferenceStatus.ACTIVE)));

        ResponseEntity<List<GuestPreferenceController.GuestPreferenceResponse>> response =
                controller.listTelegramPreferences(1773317437L, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().category()).isEqualTo(GuestPreferenceCategory.SEATING);
        assertThat(response.getBody().getFirst().preferenceText()).contains("тихий стол");
        verify(preferenceService).listActiveByChatId(1773317437L, 5);
    }

    @Test
    void softDeletesGuestPreference() {
        when(preferenceService.deletePreference(55L)).thenReturn(preference(GuestPreferenceStatus.DELETED));

        ResponseEntity<GuestPreferenceController.GuestPreferenceResponse> response =
                controller.deletePreference(55L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(55L);
        assertThat(response.getBody().status()).isEqualTo(GuestPreferenceStatus.DELETED);
        verify(preferenceService).deletePreference(55L);
    }

    private GuestPreference preference(GuestPreferenceStatus status) {
        return new GuestPreference(
                55L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                GuestPreferenceCategory.SEATING,
                "люблю тихий стол у окна",
                "TELEGRAM",
                status,
                1.0,
                "READY_FOR_DIALOG",
                "284069875",
                "{}",
                Instant.parse("2026-06-16T10:00:00Z"),
                Instant.parse("2026-06-16T10:00:00Z")
        );
    }
}
