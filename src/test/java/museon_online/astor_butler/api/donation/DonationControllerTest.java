package museon_online.astor_butler.api.donation;

import museon_online.astor_butler.domain.donation.DonationInitiative;
import museon_online.astor_butler.domain.donation.DonationOrder;
import museon_online.astor_butler.domain.donation.DonationOrderStatus;
import museon_online.astor_butler.domain.donation.DonationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonationControllerTest {

    private final DonationService donationService = mock(DonationService.class);
    private final DonationController controller = new DonationController(donationService);

    @Test
    void listsActiveInitiatives() {
        DonationInitiative initiative = new DonationInitiative(
                1L,
                "AERIS",
                "HH_GENERAL",
                "Hidden Heart / общий фонд",
                "Анонимный вклад в культурные инициативы",
                "https://qr.nspk.ru/example",
                true,
                "{}",
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
        when(donationService.listActiveInitiatives("AERIS")).thenReturn(List.of(initiative));

        ResponseEntity<List<DonationController.DonationInitiativeResponse>> response =
                controller.listInitiatives("AERIS");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().title()).isEqualTo("Hidden Heart / общий фонд");
        verify(donationService).listActiveInitiatives("AERIS");
    }

    @Test
    void confirmsDonationDraft() {
        when(donationService.confirmDraft(66L)).thenReturn(order(DonationOrderStatus.AWAITING_PAYMENT));

        ResponseEntity<DonationController.DonationOrderResponse> response = controller.confirmOrder(66L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(66L);
        assertThat(response.getBody().status()).isEqualTo(DonationOrderStatus.AWAITING_PAYMENT);
        assertThat(response.getBody().anonymous()).isTrue();
        verify(donationService).confirmDraft(66L);
    }

    @Test
    void cancelsDonationDraft() {
        when(donationService.cancelDraft(66L)).thenReturn(order(DonationOrderStatus.CANCELLED));

        ResponseEntity<DonationController.DonationOrderResponse> response = controller.cancelOrder(66L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(66L);
        assertThat(response.getBody().status()).isEqualTo(DonationOrderStatus.CANCELLED);
        verify(donationService).cancelDraft(66L);
    }

    private DonationOrder order(DonationOrderStatus status) {
        return new DonationOrder(
                66L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                1L,
                "Hidden Heart / общий фонд",
                status,
                "TELEGRAM",
                100_000L,
                "RUB",
                true,
                "Наталья Поединенко",
                "донат 1000 рублей",
                null,
                null,
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }
}
