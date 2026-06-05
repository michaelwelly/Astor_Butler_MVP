package museon_online.astor_butler.domain.booking;

public enum TableReservationStatus {
    DRAFT,
    AWAITING_GUEST_SELECTION,
    AWAITING_MANAGER_CONFIRMATION,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    EXPIRED
}
