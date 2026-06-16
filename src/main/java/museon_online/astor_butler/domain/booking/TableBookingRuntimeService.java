package museon_online.astor_butler.domain.booking;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
import museon_online.astor_butler.fsm.scenario.TableBookingDraftStorage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TableBookingRuntimeService {

    private static final String DEFAULT_VENUE_CODE = "AERIS";

    private final TableReservationService tableReservationService;
    private final TableBookingDraftStorage draftStorage;
    private final AerisMediaCatalog mediaCatalog;

    public TableBookingRuntimeView telegramRuntime(Long chatId, String venueCode) {
        String safeVenueCode = venueCode == null || venueCode.isBlank() ? DEFAULT_VENUE_CODE : venueCode.trim().toUpperCase();
        TableBookingDraftStorage.Draft draft = draftStorage.find(chatId).orElse(null);
        List<TableReservationOrder> activeReservations = tableReservationService.listActiveReservationsByChatId(chatId);
        List<TableReservationOrder> latestReservations = tableReservationService.listReservationsByChatId(chatId, 10);
        List<TableAvailability> availability = draft == null
                || draft.requestedStartAt() == null
                || draft.requestedEndAt() == null
                || draft.partySize() == null
                ? List.of()
                : tableReservationService.availability(
                        draft.venueCode() == null || draft.venueCode().isBlank() ? safeVenueCode : draft.venueCode(),
                        draft.requestedStartAt(),
                        draft.requestedEndAt(),
                        draft.partySize()
                );

        return new TableBookingRuntimeView(
                chatId,
                safeVenueCode,
                DraftView.from(draft),
                MediaAssetView.from(mediaCatalog.floorPlan()),
                tableReservationService.listTables(safeVenueCode),
                availability,
                activeReservations,
                latestReservations
        );
    }

    public record TableBookingRuntimeView(
            Long chatId,
            String venueCode,
            DraftView draft,
            MediaAssetView floorPlan,
            List<VenueTable> tables,
            List<TableAvailability> availability,
            List<TableReservationOrder> activeReservations,
            List<TableReservationOrder> latestReservations
    ) {
    }

    public record DraftView(
            String venueCode,
            Instant requestedStartAt,
            Instant requestedEndAt,
            LocalDate requestedDate,
            LocalTime requestedTime,
            Integer partySize,
            String preferredZone,
            String seatingPreference,
            String originalText
    ) {
        static DraftView from(TableBookingDraftStorage.Draft draft) {
            if (draft == null) {
                return null;
            }
            return new DraftView(
                    draft.venueCode(),
                    draft.requestedStartAt(),
                    draft.requestedEndAt(),
                    draft.requestedDate(),
                    draft.requestedTime(),
                    draft.partySize(),
                    draft.preferredZone(),
                    draft.seatingPreference(),
                    draft.originalText()
            );
        }
    }

    public record MediaAssetView(
            String assetCode,
            String venueCode,
            String domain,
            String kind,
            String title,
            String bucket,
            String objectKey,
            String filename,
            String contentType,
            boolean active
    ) {
        static MediaAssetView from(MediaAsset asset) {
            return new MediaAssetView(
                    asset.assetCode(),
                    asset.venueCode(),
                    asset.domain(),
                    asset.kind(),
                    asset.title(),
                    asset.bucket(),
                    asset.objectKey(),
                    asset.filename(),
                    asset.contentType(),
                    asset.active()
            );
        }
    }
}
