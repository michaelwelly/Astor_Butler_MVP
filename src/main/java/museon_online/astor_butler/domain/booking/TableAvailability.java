package museon_online.astor_butler.domain.booking;

public record TableAvailability(
        VenueTable table,
        boolean available,
        String reason
) {
    public static TableAvailability available(VenueTable table) {
        return new TableAvailability(table, true, "AVAILABLE");
    }

    public static TableAvailability unavailable(VenueTable table, String reason) {
        return new TableAvailability(table, false, reason);
    }
}
