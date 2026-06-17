package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class TableBookingScenario implements FsmScenario {

    private static final Pattern DATE = Pattern.compile("\\b(\\d{1,2})[./-](\\d{1,2})(?:[./-](\\d{2,4}))?\\b");
    private static final Pattern TIME = Pattern.compile("(?<![./-])\\b([01]?\\d|2[0-3])(?::([0-5]\\d)|\\s*(?:час(?:ов|а)?|ч))?\\b(?![./-])");
    private static final Pattern TABLE_NUMBER_SELECTION = Pattern.compile("^(?:стол(?:ик)?\\s*)?(?:[1-9]|1\\d)$");
    private static final Pattern TABLE_NUMBER_IN_TEXT = Pattern.compile(".*\\bстол(?:ик)?\\s*(?:[1-9]|1\\d)\\b.*");
    private static final ZoneId VENUE_ZONE = ZoneId.of("Asia/Yekaterinburg");

    private final FSMStorage fsmStorage;
    private final TableBookingDraftStorage draftStorage;
    private final TableReservationService tableReservationService;
    private final AerisMediaCatalog mediaCatalog;

    @Value("${telegram.booking.plan-pdf-asset-code:AERIS_FLOOR_PLAN}")
    private String planPdfAssetCode;

    @Value("${astor.booking.default-venue-code:AERIS}")
    private String defaultVenueCode;

    @Value("${telegram.booking.manager-chat-id:876857557}")
    private Long managerTelegramId;

    @Value("${telegram.booking.hostess-chat-id:}")
    private String hostessChatId;

    public String id() {
        return "TABLE_BOOKING";
    }

    public int priority() {
        return 30;
    }

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

        TableBookingDraftStorage.Draft draft = mergeDraft(incoming, state, normalized);
        if (draft.requestedDate() == null) {
            return askMissingSlot(incoming, state, BotState.TABLE_BOOKING_COLLECT_DATE,
                    "Конечно. На какую дату бронируем стол?",
                    "ASK_DATE");
        }
        if (draft.requestedTime() == null) {
            return askMissingSlot(incoming, state, BotState.TABLE_BOOKING_COLLECT_TIME,
                    "Принял дату. На какое время поставить бронь?",
                    "ASK_TIME");
        }
        if (draft.partySize() == null) {
            return askMissingSlot(incoming, state, BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE,
                    "На сколько гостей бронируем?",
                    "ASK_PARTY_SIZE");
        }
        return sendHallPlan(incoming, shouldSendPlanBeforeSlotCollection(state));
    }

    private OutgoingMessage askMissingSlot(IncomingMessage incoming, BotState currentState, BotState nextState, String text, String action) {
        if (shouldSendPlanBeforeSlotCollection(currentState)) {
            fsmStorage.setState(incoming.chatId(), nextState);
            return withHallPlan(
                    message(
                            incoming,
                            "Сначала отправляю план зала AERIS, чтобы вы видели пространство. " + text,
                            nextState,
                            "SEND_HALL_PLAN",
                            action
                    )
            );
        }
        fsmStorage.setState(incoming.chatId(), nextState);
        return message(incoming, text, nextState, action);
    }

    private OutgoingMessage sendHallPlan(IncomingMessage incoming) {
        return sendHallPlan(incoming, true);
    }

    private OutgoingMessage sendHallPlan(IncomingMessage incoming, boolean includeDocument) {
        fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION);
        OutgoingMessage message = message(
                incoming,
                includeDocument
                        ? "Отправляю план зала AERIS. Выберите, пожалуйста, номер стола или зону. Если хотите, напишите \"выбери сам\" — подберу подходящий вариант."
                        : "Выберите, пожалуйста, номер стола или зону на плане AERIS. Если хотите, напишите \"выбери сам\" — подберу подходящий вариант.",
                BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION,
                includeDocument ? "SEND_HALL_PLAN" : "USE_EXISTING_HALL_PLAN",
                "ASK_TABLE_SELECTION"
        );
        return includeDocument ? withHallPlan(message) : message;
    }

    private OutgoingMessage withHallPlan(OutgoingMessage message) {
        MediaAsset floorPlan = mediaCatalog.floorPlan();
        return message.withMetadata(Map.of(
                "documentAssetCode", planPdfAssetCode,
                "documentObjectKey", floorPlan.objectKey(),
                "documentFilename", floorPlan.filename(),
                "documentCaption", floorPlan.title()
        ));
    }

    private OutgoingMessage tableSelected(IncomingMessage incoming, String normalized) {
        Optional<TableBookingDraftStorage.Draft> draft = findDraft(incoming.chatId());
        if (draft.isEmpty() || draft.get().requestedStartAt() == null || draft.get().requestedEndAt() == null) {
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
                    preferredZone(normalized).orElse(draft.get().preferredZone()),
                    seatingPreference(normalized).orElse(draft.get().seatingPreference()),
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

    private boolean shouldSendPlanBeforeSlotCollection(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.UNKNOWN
                || canonical == BotState.READY_FOR_DIALOG
                || canonical == BotState.AI_FALLBACK
                || canonical == BotState.TABLE_BOOKING_INTENT
                || canonical == BotState.TABLE_BOOKING_SHOW_PLAN;
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
                requestedDate(normalized),
                requestedTime(normalized),
                partySize(normalized),
                preferredZone(normalized).orElse(null),
                seatingPreference(normalized).orElse(null),
                incoming.text()
        ));
    }

    private TableBookingDraftStorage.Draft mergeDraft(IncomingMessage incoming, BotState currentState, String normalized) {
        Optional<TableBookingDraftStorage.Draft> stored = findDraft(incoming.chatId());
        LocalDate date = extractDate(normalized).or(() -> stored.map(TableBookingDraftStorage.Draft::requestedDate)).orElse(null);
        Optional<LocalTime> extractedTime = currentState == BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE
                ? Optional.empty()
                : extractTime(normalized);
        LocalTime time = extractedTime.or(() -> stored.map(TableBookingDraftStorage.Draft::requestedTime)).orElse(null);
        Integer partySize = extractPartySize(normalized).or(() -> stored.map(TableBookingDraftStorage.Draft::partySize)).orElse(null);
        String preferredZone = preferredZone(normalized).or(() -> stored.map(TableBookingDraftStorage.Draft::preferredZone)).orElse(null);
        String seatingPreference = seatingPreference(normalized).or(() -> stored.map(TableBookingDraftStorage.Draft::seatingPreference)).orElse(null);
        String originalText = mergeOriginalText(stored.map(TableBookingDraftStorage.Draft::originalText).orElse(null), incoming.text());

        Instant startAt = date == null || time == null ? null : date.atTime(time).atZone(VENUE_ZONE).toInstant();
        TableBookingDraftStorage.Draft draft = new TableBookingDraftStorage.Draft(
                defaultVenueCode,
                startAt,
                startAt == null ? null : startAt.plusSeconds(2 * 60 * 60),
                date,
                time,
                partySize,
                preferredZone,
                seatingPreference,
                originalText
        );
        draftStorage.save(incoming.chatId(), draft);
        return draft;
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

    public boolean owns(BotState state) {
        return state != null && isTableBookingState(state.canonical());
    }

    public boolean sideEffecting() {
        return true;
    }

    private boolean hasDate(String text) {
        return extractDate(text).isPresent();
    }

    private Optional<LocalDate> extractDate(String text) {
        if (text.contains("послезавтра")) {
            return Optional.of(LocalDate.now(VENUE_ZONE).plusDays(2));
        }
        if (text.contains("сегодня")
                || text.contains("завтра")
                || DATE.matcher(text).find()) {
            return Optional.of(requestedDate(text));
        }
        return Optional.empty();
    }

    private boolean hasTime(String text) {
        return extractTime(text).isPresent();
    }

    private Optional<LocalTime> extractTime(String text) {
        if (looksLikePartySizeAnswer(text)) {
            return Optional.empty();
        }
        Matcher matcher = TIME.matcher(text);
        return matcher.find() ? Optional.of(parseTime(matcher, text)) : Optional.empty();
    }

    private boolean hasPartySize(String text) {
        return extractPartySize(text).isPresent();
    }

    private Optional<Integer> extractPartySize(String text) {
        if (text.contains("двоих") || text.contains("двоем")) {
            return Optional.of(2);
        }
        if (text.contains("двух") || text.contains("двое") || text.contains("двоём")) {
            return Optional.of(2);
        }
        if (text.contains("троих")) {
            return Optional.of(3);
        }
        if (text.contains("трое") || text.contains("трех") || text.contains("трёх")) {
            return Optional.of(3);
        }
        if (text.contains("четверых")) {
            return Optional.of(4);
        }
        if (text.contains("четверо") || text.contains("четырех") || text.contains("четырёх")) {
            return Optional.of(4);
        }
        Matcher compactMatcher = Pattern.compile("(?:^|\\s)на\\s+(\\d{1,2})\\s*(?:x|х|-х|-x)(?:\\s|$)").matcher(text);
        if (compactMatcher.find()) {
            return Optional.of(Integer.parseInt(compactMatcher.group(1)));
        }
        Matcher guestMatcher = Pattern.compile("\\b(\\d{1,2})\\s*(?:гостей|гостя|человек|персон|чел)\\b").matcher(text);
        if (guestMatcher.find()) {
            return Optional.of(Integer.parseInt(guestMatcher.group(1)));
        }
        Matcher matcher = Pattern.compile("\\bна\\s+(\\d{1,2})\\b").matcher(text);
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    private boolean looksLikePartySizeAnswer(String text) {
        return extractPartySize(text).isPresent()
                && !containsAny(text, ":", "вечер", "утр", "дня", "ноч", "час", "ч ")
                && !hasDate(text);
    }

    private boolean containsAny(String text, String... variants) {
        for (String variant : variants) {
            if (text.contains(variant)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeTableSelection(String text) {
        return text.contains("выбери сам")
                || text.contains("любой стол")
                || text.contains("любой подходящий")
                || text.contains("на твой выбор")
                || text.contains("где удобно")
                || text.contains("vip")
                || text.contains("вип")
                || text.contains("винн")
                || text.contains("бар")
                || TABLE_NUMBER_SELECTION.matcher(text).matches()
                || TABLE_NUMBER_IN_TEXT.matcher(text).matches();
    }

    private Instant requestedStartAt(String text) {
        return requestedDate(text).atTime(requestedTime(text)).atZone(VENUE_ZONE).toInstant();
    }

    private LocalDate requestedDate(String text) {
        LocalDate today = LocalDate.now(VENUE_ZONE);
        if (text.contains("послезавтра")) {
            return today.plusDays(2);
        }
        if (text.contains("завтра")) {
            return today.plusDays(1);
        }
        Matcher matcher = DATE.matcher(text);
        if (matcher.find()) {
            int day = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int year = matcher.group(3) == null ? today.getYear() : parseYear(matcher.group(3));
            LocalDate parsed = LocalDate.of(year, month, day);
            return matcher.group(3) == null && parsed.isBefore(today) ? parsed.plusYears(1) : parsed;
        }
        return today;
    }

    private LocalTime requestedTime(String text) {
        Matcher matcher = TIME.matcher(text);
        if (!matcher.find()) {
            return LocalTime.of(20, 0);
        }
        return parseTime(matcher, text);
    }

    private Integer partySize(String text) {
        return extractPartySize(text).orElse(2);
    }

    private String tableCode(String text) {
        if (text.contains("бар")) {
            return "BAR";
        }
        if (text.contains("vip") || text.contains("вип")) {
            return "13";
        }
        if (text.contains("wine") || text.contains("винн")) {
            return "7";
        }
        if (text.contains("выбери сам") || text.contains("любой") || text.contains("на твой выбор") || text.contains("где удобно")) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\b(?:стол(?:ик)?\\s*)?(1\\d|[1-9])\\b").matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Optional<String> preferredZone(String text) {
        if (text.contains("vip") || text.contains("вип")) {
            return Optional.of("VIP_ZONE");
        }
        if (text.contains("wine") || text.contains("винн")) {
            return Optional.of("WINE_ROOM");
        }
        if (text.contains("бар")) {
            return Optional.of("BAR");
        }
        return Optional.empty();
    }

    private Optional<String> seatingPreference(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        if (text.contains("тих")
                || text.contains("окн")
                || text.contains("диван")
                || text.contains("vip")
                || text.contains("вип")
                || text.contains("винн")
                || text.contains("бар")
                || text.contains("не проход")
                || text.contains("уют")) {
            return Optional.of(text);
        }
        return Optional.empty();
    }

    private LocalTime parseTime(Matcher matcher, String text) {
        int hour = Integer.parseInt(matcher.group(1));
        int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        String normalized = normalize(text);
        if (hour >= 1 && hour <= 11 && containsAny(normalized, "вечера", "вечер", "ночи")) {
            hour += 12;
        }
        return LocalTime.of(hour, minute);
    }

    private int parseYear(String value) {
        int year = Integer.parseInt(value);
        return year < 100 ? 2000 + year : year;
    }

    private String mergeOriginalText(String existing, String next) {
        String safeNext = next == null ? "" : next.trim();
        if (existing == null || existing.isBlank()) {
            return safeNext;
        }
        if (safeNext.isBlank() || existing.contains(safeNext)) {
            return existing;
        }
        return existing + " | " + safeNext;
    }

    private String guestName(IncomingMessage incoming) {
        String firstName = incoming.firstName() == null ? "" : incoming.firstName().trim();
        String lastName = incoming.lastName() == null ? "" : incoming.lastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? incoming.username() : fullName;
    }

    private Optional<TableBookingDraftStorage.Draft> findDraft(Long chatId) {
        Optional<TableBookingDraftStorage.Draft> draft = draftStorage.find(chatId);
        return draft == null ? Optional.empty() : draft;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }
}
