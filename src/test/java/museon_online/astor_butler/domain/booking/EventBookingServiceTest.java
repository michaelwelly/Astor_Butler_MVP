package museon_online.astor_butler.domain.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventBookingServiceTest {

    private final EventBookingRepository repository = mock(EventBookingRepository.class);
    private final EventBookingService service = new EventBookingService(repository);

    @Test
    void savesReadyForManagerBookingFromDraft() {
        EventBookingDraft draft = new EventBookingDraft();
        draft.setEventType("корпоратив");
        draft.setEventDate("25 мая, 19:00");
        draft.setGuestCount("40");
        draft.setEventFormat("фуршет");
        draft.setBudget("300000");
        draft.setMenuPreferences("закуски и игристое");
        draft.setTechnicalRequirements("экран и звук");
        draft.setContactDetails("Михаил, @michael_welly");
        draft.setClientComment("нужна отдельная зона");

        when(repository.save(any(EventBooking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventBooking booking = service.saveReadyForManager(42L, draft);

        assertThat(booking.getChatId()).isEqualTo(42L);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.READY_FOR_MANAGER);
        assertThat(booking.getEventType()).isEqualTo("корпоратив");
        assertThat(booking.getEventDate()).isEqualTo("25 мая, 19:00");
        assertThat(booking.getGuestCount()).isEqualTo("40");
        assertThat(booking.getEventFormat()).isEqualTo("фуршет");
        assertThat(booking.getBudget()).isEqualTo("300000");
        assertThat(booking.getMenuPreferences()).isEqualTo("закуски и игристое");
        assertThat(booking.getTechnicalRequirements()).isEqualTo("экран и звук");
        assertThat(booking.getContactDetails()).isEqualTo("Михаил, @michael_welly");
        assertThat(booking.getClientComment()).isEqualTo("нужна отдельная зона");
        verify(repository).save(any(EventBooking.class));
    }
}
