package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.domain.booking.TableReservationCommand;
import museon_online.astor_butler.domain.booking.TableReservationOrder;
import museon_online.astor_butler.domain.booking.TableReservationService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class TableBookingScenario {

    private static final Pattern TIME = Pattern.compile("\\b([01]?\\d|2[0-3])[:.]?([0-5]\\d)?\\b");
    private static final Pattern TABLE_NUMBER_SELECTION = Pattern.compile("^(?:стол(?:ик)?\\s*)?(?:[1-9]|1\\d)$");
    private static final Pattern TABLE_NUMBER_IN_TEXT = Pattern.compile(".*\\bстол(?:ик)?\\s*(?:[1-9]|1\\d)\\b.*");
    private static final ZoneId VENUE_ZONE = ZoneId.of("Asia/Yekaterinburg");

    private final FSMStorage fsmStorage;
    private final TableBookingDraftStorage draftStorage;
    private final TableReservationService tableReservationService;

    @Value("${telegram.booking.plan-pdf-path:classpath:booking/aeris-plan.pdf}")
    private String planPdfPath;

    @Value("${astor.booking.default-venue-code:AERIS}")
    private String defaultVenueCode;

    @Value("${telegram.booking.manager-chat-id:876857557}")
    private Long managerTelegramId;

    @Value("${telegram.booking.hostess-chat-id:}")
    private String hostessChatId;

    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        return isTableBookingState(state) || isTableBookingIntent(text);
    }

    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        String normalized = normalize(text);
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        if (state == BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION) {
            if (!looksLikeTableSelection(normalized)) {
                saveDraftIfComplete(incoming, normalized);
                return sendHallPlan(incoming);
            }
            return tableSelected(incoming, normalized);
        }
        if (!hasDate(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_COLLECT_DATE);
            return message(incoming, "Конечно. На какую дату бронируем стол?", BotState.TABLE_BOOKING_COLLECT_DATE, "ASK_DATE");
        }
        if (!hasTime(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_COLLECT_TIME);
            return message(incoming, "Принял дату. На какое время поставить бронь?", BotState.TABLE_BOOKING_COLLECT_TIME, "ASK_TIME");
        }
        if (!hasPartySize(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE);
            return message(incoming, "На сколько гостей бронируем?", BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE, "ASK_PARTY_SIZE");
        }
        saveDraftIfComplete(incoming, normalized);
        return sendHallPlan(incoming);
    }

    private OutgoingMessage sendHallPlan(IncomingMessage incoming) {
        fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION);
        return message(
                incoming,
                "Отправляю план зала AERIS. Выберите, пожалуйста, номер стола или зону. Если хотите, напишите \"выбери сам\" — подберу подходящий вариант.",
                BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION,
                "SEND_HALL_PLAN",
                "ASK_TABLE_SELECTION"
        ).withMetadata(Map.of(
                "documentResource", planPdfPath,
                "documentFilename", "AERIS PLAN.pdf",
                "documentCaption", "План зала AERIS"
        ));
    }

    private OutgoingMessage tableSelected(IncomingMessage incoming, String normalized) {
        Optional<TableBookingDraftStorage.Draft> draft = draftStorage.find(incoming.chatId());
        if (draft.isEmpty()) {
            fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_COLLECT_DATE);
            return message(
                    incoming,
                    "Стол понял. Напомните, пожалуйста, дату, время и количество гостей одной фразой — например: завтра в 20:00 на двоих.",
                    BotState.TABLE_BOOKING_COLLECT_DATE,
                    "ASK_BOOKING_DETAILS"
            );
        }

        try {
            TableReservationOrder order = tableReservationService.createReservation(new TableReservationCommand(
                    incoming.chatId(),
                    incoming.telegramUserId(),
                    null,
                    draft.get().venueCode(),
                    tableCode(normalized),
                    draft.get().requestedStartAt(),
                    draft.get().requestedEndAt(),
                    draft.get().partySize(),
                    guestName(incoming),
                    incoming.contactPhone(),
                    draft.get().originalText(),
                    managerTelegramId,
                    hostessChatId
            ));
            draftStorage.clear(incoming.chatId());
            fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION);
            return message(
                    incoming,
                    "Заявку #%s отправил хостес на подтверждение. Как только команда нажмет «Да» или «Нет», я сразу вернусь с ответом.".formatted(order.id()),
                    BotState.TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION,
                    "TABLE_SELECTED",
                    "RESERVATION_CREATED",
                    "WAIT_HOSTESS_CONFIRMATION"
            );
        } catch (ApiException e) {
            log.warn("Table booking reservation was not created: chatId={}, reason={}", incoming.chatId(), e.getMessage());
            fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION);
            return message(
                    incoming,
                    "Этот вариант сейчас не получается поставить в бронь. Выберите, пожалуйста, другой стол на плане или напишите «выбери сам».",
                    BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION,
                    "TABLE_SELECTION_REJECTED",
                    "ASK_TABLE_SELECTION"
            );
        }
    }

    private OutgoingMessage message(IncomingMessage incoming, String text, BotState nextState, String... actions) {
        return OutgoingMessage.of(
                incoming,
                text,
                nextState.name(),
                false,
                false,
                false,
                false,
                AdminAlert.none(),
                List.of(actions)
        );
    }

    private void saveDraftIfComplete(IncomingMessage incoming, String normalized) {
        if (!hasDate(normalized) || !hasTime(normalized) || !hasPartySize(normalized)) {
            return;
        }
        Instant startAt = requestedStartAt(normalized);
        draftStorage.save(incoming.chatId(), new TableBookingDraftStorage.Draft(
                defaultVenueCode,
                startAt,
                startAt.plusSeconds(2 * 60 * 60),
                partySize(normalized),
                incoming.text()
        ));
    }

    private boolean isTableBookingIntent(String text) {
        String value = normalize(text);
        return value.contains("забронировать стол")
                || value.contains("забронировать столик")
                || value.contains("бронь стол")
                || value.contains("бронь столик")
                || value.contains("столик")
                || value.contains("стол на")
                || value.contains("есть места");
    }

    private boolean isTableBookingState(BotState state) {
        return switch (state) {
            case TABLE_BOOKING_INTENT,
                 TABLE_BOOKING_COLLECT_DATE,
                 TABLE_BOOKING_COLLECT_TIME,
                 TABLE_BOOKING_COLLECT_PARTY_SIZE,
                 TABLE_BOOKING_SHOW_PLAN,
                 TABLE_BOOKING_WAIT_TABLE_SELECTION,
                 TABLE_BOOKING_CHANGE_REQUESTED -> true;
            default -> false;
        };
    }

    private boolean hasDate(String text) {
        return text.contains("сегодня")
                || text.contains("завтра")
                || text.matches(".*\\b\\d{1,2}[./-]\\d{1,2}([./-]\\d{2,4})?\\b.*");
    }

    private boolean hasTime(String text) {
        return TIME.matcher(text).find();
    }

    private boolean hasPartySize(String text) {
        return text.contains("двоих")
                || text.contains("двоем")
                || text.contains("троих")
                || text.contains("четверых")
                || text.matches(".*\\bна\\s+\\d{1,2}\\b.*");
    }

    private boolean looksLikeTableSelection(String text) {
        return text.contains("выбери сам")
                || text.contains("любой стол")
                || text.contains("любой подходящий")
                || text.contains("на твой выбор")
                || text.contains("где удобно")
                || text.matches(".*\\b(vip|вип)\\b.*")
                || text.matches(".*\\b(wine|винная|винный)\\b.*")
                || text.matches(".*\\b(бар|у бара|барная)\\b.*")
                || TABLE_NUMBER_SELECTION.matcher(text).matches()
                || TABLE_NUMBER_IN_TEXT.matcher(text).matches();
    }

    private Instant requestedStartAt(String text) {
        return requestedDate(text).atTime(requestedTime(text)).atZone(VENUE_ZONE).toInstant();
    }

    private LocalDate requestedDate(String text) {
        LocalDate today = LocalDate.now(VENUE_ZONE);
        if (text.contains("завтра")) {
            return today.plusDays(1);
        }
        return today;
    }

    private LocalTime requestedTime(String text) {
        java.util.regex.Matcher matcher = TIME.matcher(text);
        if (!matcher.find()) {
            return LocalTime.of(20, 0);
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        return LocalTime.of(hour, minute);
    }

    private Integer partySize(String text) {
        if (text.contains("двоих") || text.contains("двоем")) {
            return 2;
        }
        if (text.contains("троих")) {
            return 3;
        }
        if (text.contains("четверых")) {
            return 4;
        }
        java.util.regex.Matcher matcher = Pattern.compile("\\bна\\s+(\\d{1,2})\\b").matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 2;
    }

    private String tableCode(String text) {
        if (text.matches(".*\\b(бар|у бара|барная)\\b.*")) {
            return "BAR";
        }
        if (text.matches(".*\\b(vip|вип)\\b.*")) {
            return "13";
        }
        if (text.matches(".*\\b(wine|винная|винный)\\b.*")) {
            return "7";
        }
        if (text.contains("выбери сам") || text.contains("любой") || text.contains("на твой выбор") || text.contains("где удобно")) {
            return null;
        }
        java.util.regex.Matcher matcher = Pattern.compile("\\b(?:стол(?:ик)?\\s*)?(1\\d|[1-9])\\b").matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String guestName(IncomingMessage incoming) {
        String firstName = incoming.firstName() == null ? "" : incoming.firstName().trim();
        String lastName = incoming.lastName() == null ? "" : incoming.lastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? incoming.username() : fullName;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }
}
