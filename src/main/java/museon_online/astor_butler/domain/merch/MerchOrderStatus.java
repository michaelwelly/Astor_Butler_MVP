package museon_online.astor_butler.domain.merch;

public enum MerchOrderStatus {
    PENDING_TEAM,
    AWAITING_GUEST_CONFIRMATION,
    AWAITING_PAYMENT,
    PAID,
    FULFILLED,
    CANCELLED,
    REJECTED
}
