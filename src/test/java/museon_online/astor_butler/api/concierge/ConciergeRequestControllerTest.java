package museon_online.astor_butler.api.concierge;

import museon_online.astor_butler.domain.concierge.ConciergeRequest;
import museon_online.astor_butler.domain.concierge.ConciergeRequestService;
import museon_online.astor_butler.domain.concierge.ConciergeRequestStatus;
import museon_online.astor_butler.domain.concierge.ConciergeRequestType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConciergeRequestControllerTest {

    private final ConciergeRequestService requestService = mock(ConciergeRequestService.class);
    private final ConciergeRequestController controller = new ConciergeRequestController(requestService);

    @Test
    void listsTelegramConciergeRequests() {
        when(requestService.listByChatId(1773317437L, 5)).thenReturn(List.of(request(ConciergeRequestStatus.PENDING_TEAM)));

        ResponseEntity<List<ConciergeRequestController.ConciergeRequestResponse>> response =
                controller.listTelegramRequests(1773317437L, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().requestType()).isEqualTo(ConciergeRequestType.CELEBRATION);
        verify(requestService).listByChatId(1773317437L, 5);
    }

    @Test
    void marksConciergeRequestInProgress() {
        when(requestService.markInProgress(66L)).thenReturn(request(ConciergeRequestStatus.IN_PROGRESS));

        ResponseEntity<ConciergeRequestController.ConciergeRequestResponse> response = controller.markInProgress(66L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(66L);
        assertThat(response.getBody().status()).isEqualTo(ConciergeRequestStatus.IN_PROGRESS);
        verify(requestService).markInProgress(66L);
    }

    @Test
    void completesConciergeRequest() {
        when(requestService.complete(66L)).thenReturn(request(ConciergeRequestStatus.COMPLETED));

        ResponseEntity<ConciergeRequestController.ConciergeRequestResponse> response = controller.complete(66L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(ConciergeRequestStatus.COMPLETED);
        verify(requestService).complete(66L);
    }

    @Test
    void cancelsConciergeRequest() {
        when(requestService.cancel(66L)).thenReturn(request(ConciergeRequestStatus.CANCELLED));

        ResponseEntity<ConciergeRequestController.ConciergeRequestResponse> response = controller.cancel(66L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(ConciergeRequestStatus.CANCELLED);
        verify(requestService).cancel(66L);
    }

    private ConciergeRequest request(ConciergeRequestStatus status) {
        return new ConciergeRequest(
                66L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                ConciergeRequestType.CELEBRATION,
                status,
                "TELEGRAM",
                "Наталья Поединенко",
                "подготовьте свечу к десерту",
                "100500",
                "READY_FOR_DIALOG",
                "284069875",
                "{}",
                Instant.parse("2026-06-16T10:00:00Z"),
                Instant.parse("2026-06-16T10:00:00Z")
        );
    }
}
