package museon_online.astor_butler.api.auction;

import museon_online.astor_butler.domain.auction.ArtAuctionBid;
import museon_online.astor_butler.domain.auction.ArtAuctionBidStatus;
import museon_online.astor_butler.domain.auction.ArtAuctionLot;
import museon_online.astor_butler.domain.auction.ArtAuctionLotStatus;
import museon_online.astor_butler.domain.auction.ArtAuctionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtAuctionControllerTest {

    private final ArtAuctionService artAuctionService = mock(ArtAuctionService.class);
    private final ArtAuctionController controller = new ArtAuctionController(artAuctionService);

    @Test
    void listsActiveLots() {
        ArtAuctionLot lot = lot();
        when(artAuctionService.listActiveLots("AERIS")).thenReturn(List.of(lot));

        ResponseEntity<List<ArtAuctionController.ArtAuctionLotResponse>> response =
                controller.listLots("AERIS");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().title()).isEqualTo("AERIS art lot");
        verify(artAuctionService).listActiveLots("AERIS");
    }

    @Test
    void confirmsBidDraft() {
        when(artAuctionService.confirmBidDraft(77L)).thenReturn(bid(ArtAuctionBidStatus.AWAITING_MANAGER_VALIDATION));

        ResponseEntity<ArtAuctionController.ArtAuctionBidResponse> response = controller.confirmBid(77L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(77L);
        assertThat(response.getBody().status()).isEqualTo(ArtAuctionBidStatus.AWAITING_MANAGER_VALIDATION);
        verify(artAuctionService).confirmBidDraft(77L);
    }

    @Test
    void cancelsBidDraft() {
        when(artAuctionService.cancelBidDraft(77L)).thenReturn(bid(ArtAuctionBidStatus.CANCELLED));

        ResponseEntity<ArtAuctionController.ArtAuctionBidResponse> response = controller.cancelBid(77L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(77L);
        assertThat(response.getBody().status()).isEqualTo(ArtAuctionBidStatus.CANCELLED);
        verify(artAuctionService).cancelBidDraft(77L);
    }

    private ArtAuctionLot lot() {
        return new ArtAuctionLot(
                12L,
                3L,
                "LOT-1",
                "AERIS art lot",
                "AERIS guest artist",
                "Тестовый лот",
                ArtAuctionLotStatus.ACTIVE,
                1_000_000L,
                100_000L,
                "RUB",
                null,
                "{}",
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }

    private ArtAuctionBid bid(ArtAuctionBidStatus status) {
        return new ArtAuctionBid(
                77L,
                12L,
                1773317437L,
                1773317437L,
                null,
                status,
                "TELEGRAM",
                2_000_000L,
                "RUB",
                "Наталья Поединенко",
                "ставлю 20000 за картину",
                null,
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }
}
