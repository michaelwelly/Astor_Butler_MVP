package museon_online.astor_butler.telegram.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.domain.booking.EventBooking;
import museon_online.astor_butler.domain.booking.EventBookingSummaryFormatter;
import museon_online.astor_butler.telegram.utils.TelegramSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventBookingManagerNotifier {

    private final TelegramSender sender;
    private final EventBookingSummaryFormatter summaryFormatter;

    @Value("${astor.manager.telegram-chat-id:}")
    private String managerChatId;

    public void notifyReadyForManager(EventBooking booking) {
        notifyManager(booking);
    }

    public void notifyManagerReview(EventBooking booking) {
        notifyManager(booking);
    }

    private void notifyManager(EventBooking booking) {
        String summary = summaryFormatter.formatForManager(booking);
        Long chatId = parseManagerChatId();

        if (chatId == null) {
            log.info("Manager chat id is not configured. Event booking summary:\n{}", summary);
            return;
        }

        sender.sendText(chatId, summary);
        log.info("Event booking summary sent to manager chatId={} bookingId={}", chatId, booking.getId());
    }

    private Long parseManagerChatId() {
        if (managerChatId == null || managerChatId.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(managerChatId.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid astor.manager.telegram-chat-id value: {}", managerChatId);
            return null;
        }
    }
}
