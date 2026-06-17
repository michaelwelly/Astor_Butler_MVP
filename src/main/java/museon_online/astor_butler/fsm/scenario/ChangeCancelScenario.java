package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.booking.EventBookingOrder;
import museon_online.astor_butler.domain.booking.EventBookingService;
import museon_online.astor_butler.domain.booking.TableReservationOrder;
import museon_online.astor_butler.domain.booking.TableReservationService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChangeCancelScenario implements FsmScenario {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm")
            .withZone(ZoneId.of("Asia/Yekaterinburg"));

    private final FSMStorage fsmStorage;
    private final TableReservationService tableReservationService;
    private final EventBookingService eventBookingService;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    @Override
    public String id() {
        return "CHANGE_CANCEL";
    }

    @Override
    public int priority() {
        return 34;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return owns(state) || isChangeCancelIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        OutgoingMessage cancellation = tryCancelActiveOrder(incoming, currentState, text, normalized);
        if (cancellation != null) {
            return cancellation;
        }
        if (state == BotState.TABLE_BOOKING_CHANGE_REQUESTED || containsReference(normalized)) {
            return sendChangeCancelRequest(incoming, currentState, text);
        }

        List<TableReservationOrder> activeReservations = tableReservationService.listActiveReservationsByChatId(incoming.chatId());
        List<EventBookingOrder> activeEvents = eventBookingService.listActiveOrdersByChatId(incoming.chatId());
        fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_CHANGE_REQUESTED);
        if (!activeReservations.isEmpty() || !activeEvents.isEmpty()) {
            return OutgoingMessage.of(
                    incoming,
                    """
                            Нашел активные заявки по вашему диалогу:

                            %s

                            Напишите номер заявки и что сделать: отменить, перенести время, изменить количество гостей или brief мероприятия. Я не сниму бронь без явного подтверждения.
                            """.formatted(activeRequestsText(activeReservations, activeEvents)),
                    BotState.TABLE_BOOKING_CHANGE_REQUESTED.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("CHANGE_CANCEL", "ACTIVE_RESERVATIONS_FOUND", "ASK_ACTIVE_ORDER_REFERENCE")
            ).withMetadata(Map.of(
                    "scenario", id(),
                    "activeReservationIds", activeReservations.stream().map(TableReservationOrder::id).toList(),
                    "activeEventOrderIds", activeEvents.stream().map(EventBookingOrder::id).toList()
            ));
        }
        return OutgoingMessage.of(
                incoming,
                "Понял, нужно изменить или отменить бронь. Напишите, пожалуйста, дату, время или номер заявки — я передам команде точный запрос и не сниму бронь без подтверждения.",
                BotState.TABLE_BOOKING_CHANGE_REQUESTED.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("CHANGE_CANCEL", "ASK_ACTIVE_ORDER_REFERENCE")
        ).withMetadata(Map.of("scenario", id()));
    }

    private String activeRequestsText(List<TableReservationOrder> reservations, List<EventBookingOrder> events) {
        List<String> lines = new java.util.ArrayList<>();
        reservations.stream()
                .limit(5)
                .map(this::activeReservationText)
                .forEach(lines::add);
        events.stream()
                .limit(5)
                .map(this::activeEventText)
                .forEach(lines::add);
        if (lines.isEmpty()) {
            return "активных заявок не найдено";
        }
        return String.join("\n", lines);
    }

    private String activeReservationText(TableReservationOrder order) {
        return "- стол #%s: %s, %s, гостей: %s, статус: %s".formatted(
                order.id(),
                tableName(order),
                order.requestedStartAt() == null ? "время не указано" : DATE_TIME.format(order.requestedStartAt()),
                order.partySize() == null ? "не указано" : order.partySize(),
                order.status()
        );
    }

    private String activeEventText(EventBookingOrder order) {
        return "- мероприятие #%s: %s, %s, гостей: %s, статус: %s".formatted(
                order.id(),
                order.eventType() == null || order.eventType().isBlank() ? "формат уточняется" : order.eventType(),
                order.requestedDate() == null ? "дата уточняется" : order.requestedDate(),
                order.guestCount() == null ? "не указано" : order.guestCount(),
                order.status()
        );
    }

    private String tableName(TableReservationOrder order) {
        if (order.tableCode() == null || order.tableCode().isBlank()) {
            return "стол не выбран";
        }
        if (order.tableDisplayName() == null || order.tableDisplayName().isBlank()) {
            return "стол " + order.tableCode();
        }
        return order.tableDisplayName() + " (" + order.tableCode() + ")";
    }

    private OutgoingMessage tryCancelActiveOrder(
            IncomingMessage incoming,
            BotState previousState,
            String text,
            String normalized
    ) {
        if (!isCancelIntent(normalized)) {
            return null;
        }
        Long requestedId = referencedId(normalized);
        if (requestedId == null) {
            return null;
        }

        List<TableReservationOrder> activeReservations = tableReservationService.listActiveReservationsByChatId(incoming.chatId());
        for (TableReservationOrder order : activeReservations) {
            if (requestedId.equals(order.id())) {
                TableReservationOrder cancelled = tableReservationService.cancelByGuest(order.id());
                fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
                return OutgoingMessage.of(
                        incoming,
                        """
                                Готово. Я отменил бронь стола #%s и освободил слот.

                                %s
                                %s - %s

                                Если нужен новый стол или другое время, напишите новый запрос.
                                """.formatted(
                                cancelled.id(),
                                tableName(cancelled),
                                cancelled.requestedStartAt() == null ? "время не указано" : DATE_TIME.format(cancelled.requestedStartAt()),
                                cancelled.requestedEndAt() == null ? "окончание не указано" : DATE_TIME.format(cancelled.requestedEndAt())
                        ),
                        BotState.READY_FOR_DIALOG.name(),
                        false,
                        false,
                        true,
                        false,
                        AdminAlert.none(),
                        List.of("CHANGE_CANCEL", "TABLE_RESERVATION_CANCELLED", "HOLD_RELEASED", "RETURN_MAIN_MENU")
                ).withMetadata(Map.of(
                        "scenario", id(),
                        "cancelledTableReservationId", cancelled.id()
                ));
            }
        }

        List<EventBookingOrder> activeEvents = eventBookingService.listActiveOrdersByChatId(incoming.chatId());
        for (EventBookingOrder order : activeEvents) {
            if (requestedId.equals(order.id())) {
                EventBookingOrder cancelled = eventBookingService.cancelByGuest(order.id());
                fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
                return OutgoingMessage.of(
                        incoming,
                        """
                                Готово. Я отметил event-заявку #%s как отмененную.

                                Формат: %s
                                Дата: %s

                                Команда увидит изменение в журнале. Если нужно создать новую заявку, напишите детали заново.
                                """.formatted(
                                cancelled.id(),
                                cancelled.eventType() == null || cancelled.eventType().isBlank() ? "уточнялся" : cancelled.eventType(),
                                cancelled.requestedDate() == null ? "уточнялась" : cancelled.requestedDate()
                        ),
                        BotState.READY_FOR_DIALOG.name(),
                        false,
                        false,
                        true,
                        false,
                        adminAlert(incoming, previousState, text),
                        List.of("CHANGE_CANCEL", "EVENT_BOOKING_CANCELLED", "ADMIN_ALERT", "RETURN_MAIN_MENU")
                ).withMetadata(Map.of(
                        "scenario", id(),
                        "cancelledEventBookingId", cancelled.id()
                ));
            }
        }

        return null;
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.TABLE_BOOKING_CHANGE_REQUESTED;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage sendChangeCancelRequest(IncomingMessage incoming, BotState previousState, String text) {
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Передал запрос команде. Бронь не меняю и не отменяю автоматически: менеджер проверит активную заявку и подтвердит следующий шаг.",
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                adminAlert(incoming, previousState, text),
                List.of("CHANGE_CANCEL", "ADMIN_ALERT", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "changeCancelRequest", text == null ? "" : text.trim()
        ));
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState previousState, String text) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }
        String body = """
                <b>Astor Butler / change or cancel</b>
                Гость просит изменить или отменить бронь

                <b>%s</b>
                chat %s / user %s%s

                <b>Сообщение гостя</b>
                <blockquote>%s</blockquote>

                <b>Контекст</b>
                Previous state: %s
                Scenario: CHANGE_CANCEL

                <b>Действие</b>
                Найдите активную бронь/заявку. Не отменяйте без явного подтверждения гостя и команды.

                <b>Техника</b>
                Channel: %s
                Correlation: %s
                """.formatted(
                html(displayName(incoming)),
                html(text(incoming.chatId())),
                html(text(incoming.telegramUserId())),
                incoming.username() == null || incoming.username().isBlank() ? "" : " / @" + html(incoming.username()),
                html(blankAsEmptyLabel(text)),
                html(text(previousState)),
                html(text(incoming.channel())),
                html(blankAsEmptyLabel(incoming.correlationId()))
        );
        return new AdminAlert(true, adminChatId, body);
    }

    private boolean isChangeCancelIntent(String text) {
        return containsAny(
                text,
                "отменить брон",
                "отмена брон",
                "отменить стол",
                "отменить заявку",
                "перенести брон",
                "изменить брон",
                "поменять брон",
                "изменить время",
                "поменять время",
                "перенести время",
                "не придем",
                "не придём",
                "не сможем",
                "опоздаем",
                "задержимся"
        );
    }

    private boolean isCancelIntent(String text) {
        return containsAny(text, "отмен", "не придем", "не придём", "не сможем");
    }

    private Long referencedId(String text) {
        java.util.regex.Matcher hash = java.util.regex.Pattern.compile("#\\s*(\\d{1,8})").matcher(text);
        if (hash.find()) {
            return Long.parseLong(hash.group(1));
        }
        java.util.regex.Matcher order = java.util.regex.Pattern
                .compile("(?:заказ|заявк[ауи]|бронь|брон[ьи])\\s*(?:номер|№|#)?\\s*(\\d{1,8})")
                .matcher(text);
        if (order.find()) {
            return Long.parseLong(order.group(1));
        }
        return null;
    }

    private boolean containsReference(String text) {
        return text.matches(".*\\b\\d{1,2}[:.]\\d{2}\\b.*")
                || text.matches(".*\\b\\d{1,2}[./-]\\d{1,2}.*")
                || text.matches(".*\\b\\d{3,}\\b.*")
                || containsAny(text, "сегодня", "завтра", "послезавтра", "вечером", "обед", "ужин");
    }

    private boolean containsAny(String text, String... variants) {
        for (String variant : variants) {
            if (text.contains(variant)) {
                return true;
            }
        }
        return false;
    }

    private String displayName(IncomingMessage incoming) {
        String firstName = normalizeDisplay(incoming.firstName());
        String lastName = normalizeDisplay(incoming.lastName());
        String username = normalizeDisplay(incoming.username());
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (!username.isBlank()) {
            return "@" + username;
        }
        return "unknown";
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplay(String text) {
        return text == null ? "" : text.trim();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blankAsEmptyLabel(String value) {
        String normalized = normalizeDisplay(value);
        return normalized.isBlank() ? "(empty)" : normalized;
    }

    private String html(String value) {
        return text(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
