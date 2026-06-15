package museon_online.astor_butler.domain.donation;

public enum DonationOrderStatus {
    DRAFT,
    AWAITING_GUEST_CONFIRMATION,
    AWAITING_PAYMENT,
    PAID,
    CANCELLED,
    FAILED
}
