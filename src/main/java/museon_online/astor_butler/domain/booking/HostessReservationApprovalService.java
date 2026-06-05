package museon_online.astor_butler.domain.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.service.message.IncomingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostessReservationApprovalService {

    private static final Pattern CALLBACK = Pattern.compile("^table_booking:(confirm|reject):(\\d+)$");

    private final TableReservationService tableReservationService;
    private final TableReservationNotificationService notificationService;

    @Value("${telegram.booking.hostess-chat-id:}")
    private String hostessChatId;

    @Transactional
    public boolean handle(IncomingMessage incoming) {
        if (!isHostessChat(incoming)) {
            return false;
        }

        log.debug("Hostess chat text message consumed by table approval boundary: {}", incoming.text());
        return true;
    }

    @Transactional
    public CallbackResult handleCallback(String callbackData, Long chatId) {
        if (!isHostessChat(chatId)) {
            return CallbackResult.notHandled();
        }

        java.util.regex.Matcher matcher = CALLBACK.matcher(callbackData == null ? "" : callbackData);
        if (!matcher.matches()) {
            return CallbackResult.notHandled();
        }

        String action = matcher.group(1);
        Long orderId = Long.parseLong(matcher.group(2));

        if ("confirm".equals(action)) {
            TableReservationOrder confirmed = tableReservationService.confirm(orderId);
            notificationService.notifyHostessAcknowledged(confirmed);
            return CallbackResult.handled("Бронь #" + orderId + " подтверждена");
        }

        TableReservationOrder rejected = tableReservationService.reject(orderId);
        notificationService.notifyHostessRejected(rejected);
        return CallbackResult.handled("Бронь #" + orderId + " отменена");
    }

    private boolean isHostessChat(IncomingMessage incoming) {
        if (incoming == null || incoming.chatId() == null || hostessChatId == null || hostessChatId.isBlank()) {
            return false;
        }
        return hostessChatId.equals(incoming.chatId().toString());
    }

    private boolean isHostessChat(Long chatId) {
        if (chatId == null || hostessChatId == null || hostessChatId.isBlank()) {
            return false;
        }
        return hostessChatId.equals(chatId.toString());
    }

    public record CallbackResult(boolean handled, String answerText) {
        static CallbackResult handled(String answerText) {
            return new CallbackResult(true, answerText);
        }

        static CallbackResult notHandled() {
            return new CallbackResult(false, "");
        }
    }
}
