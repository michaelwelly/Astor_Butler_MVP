package museon_online.astor_butler.domain.tip;

public enum TipOrderStatus {
    DRAFT,
    AWAITING_GUEST_CONFIRMATION,
    AWAITING_PAYMENT,
    PAID,
    CANCELLED,
    FAILED
}
