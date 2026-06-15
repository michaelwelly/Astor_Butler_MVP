package museon_online.astor_butler.domain.auction;

public enum ArtAuctionBidStatus {
    DRAFT,
    AWAITING_GUEST_CONFIRMATION,
    AWAITING_MANAGER_VALIDATION,
    ACTIVE,
    OUTBID,
    WINNER_PENDING_PAYMENT,
    PAID,
    CANCELLED,
    REJECTED
}
