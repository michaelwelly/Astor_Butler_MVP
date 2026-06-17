package museon_online.astor_butler.api.tip;

import museon_online.astor_butler.domain.tip.StaffProfile;
import museon_online.astor_butler.domain.tip.TipOrder;
import museon_online.astor_butler.domain.tip.TipOrderStatus;
import museon_online.astor_butler.domain.tip.TipService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TipControllerTest {

    private final TipService tipService = mock(TipService.class);
    private final TipController controller = new TipController(tipService);

    @Test
    void listsActiveStaffProfiles() {
        StaffProfile staff = new StaffProfile(
                1L,
                "AERIS",
                "Команда AERIS",
                "VENUE_TEAM",
                876857557L,
                "+79991234567",
                "https://qr.nspk.ru/example",
                true,
                "{}",
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
        when(tipService.listActiveStaff("AERIS")).thenReturn(List.of(staff));

        ResponseEntity<List<TipController.StaffProfileResponse>> response = controller.listStaff("AERIS");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().displayName()).isEqualTo("Команда AERIS");
        verify(tipService).listActiveStaff("AERIS");
    }

    @Test
    void confirmsTipDraft() {
        when(tipService.confirmDraft(55L)).thenReturn(order(TipOrderStatus.AWAITING_PAYMENT));

        ResponseEntity<TipController.TipOrderResponse> response = controller.confirmOrder(55L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(55L);
        assertThat(response.getBody().status()).isEqualTo(TipOrderStatus.AWAITING_PAYMENT);
        verify(tipService).confirmDraft(55L);
    }

    @Test
    void cancelsTipDraft() {
        when(tipService.cancelDraft(55L)).thenReturn(order(TipOrderStatus.CANCELLED));

        ResponseEntity<TipController.TipOrderResponse> response = controller.cancelOrder(55L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(55L);
        assertThat(response.getBody().status()).isEqualTo(TipOrderStatus.CANCELLED);
        verify(tipService).cancelDraft(55L);
    }

    private TipOrder order(TipOrderStatus status) {
        return new TipOrder(
                55L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                1L,
                "Команда AERIS",
                status,
                "TELEGRAM",
                100_000L,
                "RUB",
                "Наталья Поединенко",
                "оставить чаевые 1000 рублей",
                null,
                null,
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }
}
