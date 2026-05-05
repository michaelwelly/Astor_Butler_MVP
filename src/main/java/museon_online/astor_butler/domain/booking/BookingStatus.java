package museon_online.astor_butler.domain.booking;

public enum BookingStatus {
    DRAFT,
    COLLECTING_DETAILS,
    WAITING_CLIENT_CONFIRMATION,
    READY_FOR_MANAGER,
    MANAGER_REVIEW,
    NEEDS_CLIENT_CLARIFICATION,
    PREBOOKED,
    CONFIRMED,
    CANCELLED
}
