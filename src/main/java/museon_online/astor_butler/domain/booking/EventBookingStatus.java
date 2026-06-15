package museon_online.astor_butler.domain.booking;

public enum EventBookingStatus {
    DRAFT,
    AWAITING_MANAGER_REVIEW,
    MANAGER_CLARIFICATION_REQUESTED,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    COMPLETED
}
