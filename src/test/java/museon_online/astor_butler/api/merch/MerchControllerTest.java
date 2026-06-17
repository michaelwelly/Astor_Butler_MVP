package museon_online.astor_butler.api.merch;

import museon_online.astor_butler.domain.merch.MerchItem;
import museon_online.astor_butler.domain.merch.MerchItemStatus;
import museon_online.astor_butler.domain.merch.MerchOrder;
import museon_online.astor_butler.domain.merch.MerchOrderStatus;
import museon_online.astor_butler.domain.merch.MerchService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MerchControllerTest {

    private final MerchService merchService = mock(MerchService.class);
    private final MerchController controller = new MerchController(merchService);

    @Test
    void listsActiveMerchItems() {
        MerchItem item = new MerchItem(
                7L,
                "AERIS",
                "SABRAGE_CHAIN",
                "Сабражная цепь AERIS",
                "Памятный hospitality-артефакт",
                MerchItemStatus.ACTIVE,
                12_000_00L,
                "RUB",
                "manual",
                "aeris-merch-sabrage-chain",
                "{}",
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
        when(merchService.listActiveItems("AERIS")).thenReturn(List.of(item));

        ResponseEntity<List<MerchController.MerchItemResponse>> response = controller.listItems("AERIS");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().title()).isEqualTo("Сабражная цепь AERIS");
        verify(merchService).listActiveItems("AERIS");
    }

    @Test
    void confirmsMerchDraft() {
        when(merchService.confirmDraft(77L)).thenReturn(order(MerchOrderStatus.PENDING_TEAM));

        ResponseEntity<MerchController.MerchOrderResponse> response = controller.confirmOrder(77L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(77L);
        assertThat(response.getBody().status()).isEqualTo(MerchOrderStatus.PENDING_TEAM);
        verify(merchService).confirmDraft(77L);
    }

    @Test
    void cancelsMerchDraft() {
        when(merchService.cancelDraft(77L)).thenReturn(order(MerchOrderStatus.CANCELLED));

        ResponseEntity<MerchController.MerchOrderResponse> response = controller.cancelOrder(77L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(77L);
        assertThat(response.getBody().status()).isEqualTo(MerchOrderStatus.CANCELLED);
        verify(merchService).cancelDraft(77L);
    }

    private MerchOrder order(MerchOrderStatus status) {
        return new MerchOrder(
                77L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                7L,
                "Сабражная цепь AERIS",
                status,
                "TELEGRAM",
                1,
                12_000_00L,
                "RUB",
                "Наталья Поединенко",
                "хочу купить сабражную цепь",
                "TEAM_CONFIRMATION_REQUIRED",
                null,
                "{}",
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }
}
