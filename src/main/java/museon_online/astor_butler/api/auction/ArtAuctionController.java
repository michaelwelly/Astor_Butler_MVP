package museon_online.astor_butler.api.auction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.auction.ArtAuctionBid;
import museon_online.astor_butler.domain.auction.ArtAuctionBidCommand;
import museon_online.astor_butler.domain.auction.ArtAuctionBidStatus;
import museon_online.astor_butler.domain.auction.ArtAuctionLot;
import museon_online.astor_butler.domain.auction.ArtAuctionLotStatus;
import museon_online.astor_butler.domain.auction.ArtAuctionService;
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
@RequestMapping("/api/auctions")
@Tag(name = "Art Auction API", description = "Event art lots and bid drafts")
@RequiredArgsConstructor
public class ArtAuctionController {

    private final ArtAuctionService artAuctionService;

    @GetMapping("/lots")
    @Operation(summary = "List active art auction lots")
    public ResponseEntity<List<ArtAuctionLotResponse>> listLots(
            @RequestParam(name = "venueCode", defaultValue = "AERIS") String venueCode
    ) {
        return ResponseEntity.ok(artAuctionService.listActiveLots(venueCode).stream()
                .map(ArtAuctionLotResponse::from)
                .toList());
    }

    @PostMapping("/bids")
    @Operation(summary = "Create art auction bid draft")
    public ResponseEntity<ArtAuctionBidResponse> createBid(@RequestBody ArtAuctionBidCreateRequest request) {
        ArtAuctionBid bid = artAuctionService.createBidDraft(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ArtAuctionBidResponse.from(bid));
    }

    @GetMapping("/bids/{id}")
    @Operation(summary = "Get art auction bid")
    public ResponseEntity<ArtAuctionBidResponse> getBid(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ArtAuctionBidResponse.from(artAuctionService.getBid(id)));
    }

    @PostMapping("/bids/{id}/confirm")
    @Operation(summary = "Confirm art auction bid draft and move it to manager validation")
    public ResponseEntity<ArtAuctionBidResponse> confirmBid(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ArtAuctionBidResponse.from(artAuctionService.confirmBidDraft(id)));
    }

    @PostMapping("/bids/{id}/cancel")
    @Operation(summary = "Cancel art auction bid draft")
    public ResponseEntity<ArtAuctionBidResponse> cancelBid(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ArtAuctionBidResponse.from(artAuctionService.cancelBidDraft(id)));
    }

    @GetMapping("/bids/telegram/{chatId}")
    @Operation(summary = "List recent art auction bids for Telegram chat")
    public ResponseEntity<List<ArtAuctionBidResponse>> listTelegramBids(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(artAuctionService.listBidsByChatId(chatId, limit).stream()
                .map(ArtAuctionBidResponse::from)
                .toList());
    }

    public record ArtAuctionBidCreateRequest(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            Long lotId,
            Long amountMinor,
            String currency,
            String bidderName,
            String guestComment
    ) {
        ArtAuctionBidCommand toCommand() {
            return new ArtAuctionBidCommand(
                    chatId,
                    telegramUserId,
                    userId,
                    venueCode,
                    lotId,
                    amountMinor,
                    currency,
                    bidderName,
                    guestComment
            );
        }
    }

    public record ArtAuctionLotResponse(
            Long id,
            Long auctionEventId,
            String lotCode,
            String title,
            String artistName,
            String description,
            ArtAuctionLotStatus status,
            Long startingPriceMinor,
            Long minStepMinor,
            String currency,
            String mediaAssetCode
    ) {
        static ArtAuctionLotResponse from(ArtAuctionLot lot) {
            return new ArtAuctionLotResponse(
                    lot.id(),
                    lot.auctionEventId(),
                    lot.lotCode(),
                    lot.title(),
                    lot.artistName(),
                    lot.description(),
                    lot.status(),
                    lot.startingPriceMinor(),
                    lot.minStepMinor(),
                    lot.currency(),
                    lot.mediaAssetCode()
            );
        }
    }

    public record ArtAuctionBidResponse(
            Long id,
            Long lotId,
            Long chatId,
            Long telegramUserId,
            Long userId,
            ArtAuctionBidStatus status,
            Long amountMinor,
            String currency,
            String bidderName,
            String guestComment,
            String paymentExternalId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static ArtAuctionBidResponse from(ArtAuctionBid bid) {
            return new ArtAuctionBidResponse(
                    bid.id(),
                    bid.lotId(),
                    bid.chatId(),
                    bid.telegramUserId(),
                    bid.userId(),
                    bid.status(),
                    bid.amountMinor(),
                    bid.currency(),
                    bid.bidderName(),
                    bid.guestComment(),
                    bid.paymentExternalId(),
                    bid.createdAt(),
                    bid.updatedAt()
            );
        }
    }
}
