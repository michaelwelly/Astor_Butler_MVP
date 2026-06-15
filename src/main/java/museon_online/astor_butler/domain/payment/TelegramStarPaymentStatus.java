package museon_online.astor_butler.domain.payment;

public enum TelegramStarPaymentStatus {
    DRAFT,
    INVOICE_SENT,
    PRE_CHECKOUT_APPROVED,
    PRE_CHECKOUT_REJECTED,
    PAID,
    REFUND_REQUESTED,
    REFUNDED,
    FAILED,
    CANCELLED
}
