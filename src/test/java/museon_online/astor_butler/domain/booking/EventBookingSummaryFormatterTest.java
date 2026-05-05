package museon_online.astor_butler.domain.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventBookingSummaryFormatterTest {

    private final EventBookingSummaryFormatter formatter = new EventBookingSummaryFormatter();

    @Test
    void formatsBookingForManager() {
        EventBooking booking = EventBooking.builder()
                .id(7L)
                .chatId(42L)
                .status(BookingStatus.READY_FOR_MANAGER)
                .eventType("корпоратив")
                .eventDate("25 мая, 19:00")
                .guestCount("40")
                .eventFormat("фуршет")
                .budget("300000")
                .menuPreferences("закуски и игристое")
                .technicalRequirements("экран и звук")
                .contactDetails("Михаил, @michael_welly")
                .clientComment("нужна отдельная зона")
                .build();

        String summary = formatter.formatForManager(booking);

        assertThat(summary)
                .contains("Новая заявка на мероприятие")
                .contains("ID: 7")
                .contains("Статус: READY_FOR_MANAGER")
                .contains("Мероприятие: корпоратив")
                .contains("Контакт: Михаил, @michael_welly");
    }
}
