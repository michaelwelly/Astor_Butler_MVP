package museon_online.astor_butler.domain.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventBookingService {

    private final EventBookingRepository repository;

    public EventBooking saveReadyForManager(Long chatId, EventBookingDraft draft) {
        return save(chatId, draft, BookingStatus.READY_FOR_MANAGER);
    }

    public EventBooking saveManagerReview(Long chatId, EventBookingDraft draft) {
        return save(chatId, draft, BookingStatus.MANAGER_REVIEW);
    }

    private EventBooking save(Long chatId, EventBookingDraft draft, BookingStatus status) {
        EventBooking booking = EventBooking.builder()
                .chatId(chatId)
                .status(status)
                .eventType(draft.getEventType())
                .eventDate(draft.getEventDate())
                .guestCount(draft.getGuestCount())
                .eventFormat(draft.getEventFormat())
                .budget(draft.getBudget())
                .menuPreferences(draft.getMenuPreferences())
                .technicalRequirements(draft.getTechnicalRequirements())
                .contactDetails(draft.getContactDetails())
                .clientComment(draft.getClientComment())
                .build();

        EventBooking saved = repository.save(booking);
        log.info("Saved event booking id={} status={} chatId={}", saved.getId(), saved.getStatus(), chatId);
        return saved;
    }
}
