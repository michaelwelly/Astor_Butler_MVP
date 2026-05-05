package museon_online.astor_butler.domain.booking;

public interface EventBookingDraftStorage {

    EventBookingDraft getOrCreate(Long chatId);

    void save(Long chatId, EventBookingDraft draft);

    void clear(Long chatId);
}
