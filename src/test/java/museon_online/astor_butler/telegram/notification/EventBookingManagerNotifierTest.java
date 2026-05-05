package museon_online.astor_butler.telegram.notification;

import museon_online.astor_butler.domain.booking.EventBooking;
import museon_online.astor_butler.domain.booking.EventBookingSummaryFormatter;
import museon_online.astor_butler.telegram.utils.TelegramSender;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EventBookingManagerNotifierTest {

    private final TelegramSender sender = mock(TelegramSender.class);
    private final EventBookingManagerNotifier notifier =
            new EventBookingManagerNotifier(sender, new EventBookingSummaryFormatter());

    @Test
    void sendsSummaryWhenManagerChatIdIsConfigured() {
        ReflectionTestUtils.setField(notifier, "managerChatId", "100500");

        notifier.notifyReadyForManager(EventBooking.builder()
                .id(7L)
                .chatId(42L)
                .eventType("банкет")
                .build());

        verify(sender).sendText(eq(100500L), contains("Новая заявка на мероприятие"));
    }

    @Test
    void skipsTelegramSendWhenManagerChatIdIsMissing() {
        ReflectionTestUtils.setField(notifier, "managerChatId", "");

        notifier.notifyReadyForManager(EventBooking.builder()
                .id(7L)
                .chatId(42L)
                .eventType("банкет")
                .build());

        verify(sender, never()).sendText(eq(100500L), contains("Новая заявка"));
    }
}
