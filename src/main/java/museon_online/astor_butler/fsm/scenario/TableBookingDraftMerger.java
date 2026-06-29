package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.understanding.SlotValue;
import museon_online.astor_butler.fsm.understanding.UnderstoodInput;
import museon_online.astor_butler.service.message.IncomingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TableBookingDraftMerger {

    private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern DATE = Pattern.compile("\\b(\\d{1,2})[./-](\\d{1,2})(?:[./-](\\d{2,4}))?\\b");
    private static final Pattern TIME = Pattern.compile("(?<![./-])\\b([01]?\\d|2[0-3])(?::([0-5]\\d)|\\s*(?:час(?:ов|а)?|ч))?\\b(?![./-])");
    private static final Pattern TABLE_NUMBER_SELECTION = Pattern.compile("^(?:стол(?:ик)?\\s*)?(?:[1-9]|1\\d)$");
    private static final Pattern TABLE_NUMBER_IN_TEXT = Pattern.compile(".*(?:^|\\s)стол(?:ик)?\\s*(?:[1-9]|1\\d)(?:\\s|$).*");
    private static final Pattern TABLE_NUMBER_BEFORE_WORD = Pattern.compile(".*(?:^|\\s)(?:[1-9]|1\\d)\\s*стол(?:ик)?(?:\\s|$).*");
    private static final Pattern SHORT_PARTY_SIZE_ANSWER = Pattern.compile("^(?:на\\s*)?(\\d{1,2})(?:\\s*(?:гостей|гостя|человек|персон|чел))?$");
    private final TableBookingDraftStorage draftStorage;
    private final BookingTimeProvider timeProvider;

    @Value("${astor.booking.default-venue-code:AERIS}")
    private String defaultVenueCode;

    public TableBookingDraftMerger(TableBookingDraftStorage draftStorage, BookingTimeProvider timeProvider) {
        this.draftStorage = draftStorage;
        this.timeProvider = timeProvider;
    }

    public TableBookingDraftStorage.Draft merge(
            IncomingMessage incoming,
            BotState currentState,
            String normalized,
            UnderstoodInput understood
    ) {
        Optional<TableBookingDraftStorage.Draft> stored = findDraft(incoming.chatId());
        Map<String, SlotValue> slots = understood == null || understood.slots() == null ? Map.of() : understood.slots();

        Optional<LocalDate> extractedDate = dateFromSlot(slots).or(() -> extractDate(normalized));
        LocalDate date = extractedDate.or(() -> storedDate(stored)).orElse(null);

        Optional<LocalTime> extractedTime = shouldIgnoreTimeInCurrentStep(currentState, normalized, extractedDate)
                ? Optional.empty()
                : timeFromSlot(slots).or(() -> extractTime(normalized));
        LocalTime time = extractedTime.or(() -> storedTime(stored)).orElse(null);

        Integer partySize = partySizeFromSlot(slots)
                .or(() -> extractPartySize(normalized, currentState))
                .or(() -> stored.map(TableBookingDraftStorage.Draft::partySize))
                .orElse(null);

        boolean captureTableSelection = shouldCaptureTableSelection(currentState, normalized, slots);
        String tableCode = captureTableSelection
                ? tableCodeFromSlot(slots).orElseGet(() -> tableCode(normalized))
                : stored.map(TableBookingDraftStorage.Draft::tableCode).orElse(null);

        boolean captureSeatingPreference = shouldCaptureSeatingPreference(currentState, normalized, slots);
        String preferredZone = (captureTableSelection || captureSeatingPreference
                ? preferredZoneFromSlot(slots).or(() -> preferredZone(normalized))
                : Optional.<String>empty())
                .or(() -> stored.map(TableBookingDraftStorage.Draft::preferredZone))
                .orElse(null);
        String seatingPreference = (captureTableSelection || captureSeatingPreference
                ? seatingPreferenceFromSlot(slots).or(() -> seatingPreference(normalized, captureSeatingPreference))
                : Optional.<String>empty())
                .or(() -> stored.map(TableBookingDraftStorage.Draft::seatingPreference))
                .orElse(null);

        String originalText = mergeOriginalText(stored.map(TableBookingDraftStorage.Draft::originalText).orElse(null), incoming.text());
        Instant startAt = date == null || time == null ? null : date.atTime(time).atZone(BookingTimeProvider.VENUE_ZONE).toInstant();
        TableBookingDraftStorage.Draft draft = new TableBookingDraftStorage.Draft(
                defaultVenueCode,
                startAt,
                startAt == null ? null : startAt.plusSeconds(2 * 60 * 60),
                date,
                time,
                partySize,
                tableCode,
                preferredZone,
                seatingPreference,
                seatingPreferenceResolved(currentState, normalized, seatingPreference, stored, slots),
                originalText
        );
        draftStorage.save(incoming.chatId(), draft);
        return draft;
    }

    private Optional<LocalDate> dateFromSlot(Map<String, SlotValue> slots) {
        return slot(slots, "date").map(SlotValue::value).flatMap(this::extractDate);
    }

    private Optional<LocalTime> timeFromSlot(Map<String, SlotValue> slots) {
        return slot(slots, "time").map(SlotValue::value).flatMap(this::extractTime);
    }

    private Optional<Integer> partySizeFromSlot(Map<String, SlotValue> slots) {
        return slot(slots, "partySize").map(SlotValue::value).flatMap(value -> {
            try {
                int parsed = Integer.parseInt(value.trim());
                return parsed > 0 && parsed <= 20 ? Optional.of(parsed) : Optional.empty();
            } catch (NumberFormatException ignored) {
                return extractPartySize(normalize(value));
            }
        });
    }

    private Optional<String> tableCodeFromSlot(Map<String, SlotValue> slots) {
        return slot(slots, "tableNumber").map(SlotValue::value).map(String::trim).filter(value -> !value.isBlank());
    }

    private Optional<String> preferredZoneFromSlot(Map<String, SlotValue> slots) {
        return seatingPreferenceFromSlot(slots).flatMap(this::preferredZone);
    }

    private Optional<String> seatingPreferenceFromSlot(Map<String, SlotValue> slots) {
        return slot(slots, "seatingPreference")
                .map(SlotValue::value)
                .map(this::normalize)
                .filter(value -> !value.isBlank() && !isNoSeatingPreference(value));
    }

    private Optional<SlotValue> slot(Map<String, SlotValue> slots, String name) {
        return Optional.ofNullable(slots.get(name));
    }

    private boolean seatingPreferenceResolved(
            BotState currentState,
            String normalized,
            String seatingPreference,
            Optional<TableBookingDraftStorage.Draft> stored,
            Map<String, SlotValue> slots
    ) {
        if (seatingPreference != null && !seatingPreference.isBlank()) {
            return true;
        }
        if (stored.map(TableBookingDraftStorage.Draft::seatingPreferenceResolved).orElse(false)) {
            return true;
        }
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        return state == BotState.TABLE_BOOKING_COLLECT_SEATING_PREFERENCE
                && (isNoSeatingPreference(normalized) || seatingPreferenceFromSlot(slots).isEmpty() && slot(slots, "seatingPreference").isPresent());
    }

    private Optional<LocalDate> storedDate(Optional<TableBookingDraftStorage.Draft> stored) {
        return stored.flatMap(draft -> draft.requestedDate() == null
                ? Optional.ofNullable(draft.requestedStartAt()).map(startAt -> startAt.atZone(BookingTimeProvider.VENUE_ZONE).toLocalDate())
                : Optional.of(draft.requestedDate()));
    }

    private Optional<LocalTime> storedTime(Optional<TableBookingDraftStorage.Draft> stored) {
        return stored.flatMap(draft -> draft.requestedTime() == null
                ? Optional.ofNullable(draft.requestedStartAt()).map(startAt -> startAt.atZone(BookingTimeProvider.VENUE_ZONE).toLocalTime())
                : Optional.of(draft.requestedTime()));
    }

    private boolean shouldIgnoreTimeInCurrentStep(BotState currentState, String normalized, Optional<LocalDate> extractedDate) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        if (state == BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE || state == BotState.TABLE_BOOKING_COLLECT_SEATING_PREFERENCE) {
            return true;
        }
        if (looksLikeTableSelection(normalized)) {
            return true;
        }
        return state == BotState.TABLE_BOOKING_COLLECT_DATE && extractedDate.isEmpty();
    }

    private boolean shouldCaptureTableSelection(BotState currentState, String normalized, Map<String, SlotValue> slots) {
        if (slots.containsKey("tableNumber")) {
            return true;
        }
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        if (state == BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION || state == BotState.TABLE_BOOKING_SHOW_PLAN) {
            return looksLikeTableSelection(normalized);
        }
        return containsAny(normalized, "стол", "столик", "винн", "vip", "вип", "бар", "выбери сам", "любой стол", "любой подходящий", "на твой выбор");
    }

    private boolean shouldCaptureSeatingPreference(BotState currentState, String normalized, Map<String, SlotValue> slots) {
        if (slots.containsKey("seatingPreference")) {
            return true;
        }
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        return state == BotState.TABLE_BOOKING_COLLECT_SEATING_PREFERENCE
                || containsAny(normalized, "тих", "окн", "диван", "не проход", "уют");
    }

    private Optional<LocalDate> extractDate(String text) {
        if (ISO_DATE.matcher(text).matches()) {
            return Optional.of(LocalDate.parse(text));
        }
        if (text.contains("послезавтра")) {
            return Optional.of(timeProvider.today().plusDays(2));
        }
        Optional<LocalDate> weekday = extractWeekdayDate(text);
        if (weekday.isPresent()) {
            return weekday;
        }
        if (text.contains("сегодня") || text.contains("завтра") || DATE.matcher(text).find()) {
            return Optional.of(requestedDate(text));
        }
        return Optional.empty();
    }

    private Optional<LocalDate> extractWeekdayDate(String text) {
        DayOfWeek dayOfWeek = weekdayFromText(text).orElse(null);
        if (dayOfWeek == null) {
            return Optional.empty();
        }
        LocalDate today = timeProvider.today();
        LocalDate date = today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
        return Optional.of(date);
    }

    private Optional<DayOfWeek> weekdayFromText(String text) {
        if (containsAny(text, "понедельник", "понедельника")) {
            return Optional.of(DayOfWeek.MONDAY);
        }
        if (containsAny(text, "вторник", "вторника")) {
            return Optional.of(DayOfWeek.TUESDAY);
        }
        if (containsAny(text, "среду", "среда", "среды")) {
            return Optional.of(DayOfWeek.WEDNESDAY);
        }
        if (containsAny(text, "четверг", "четверга")) {
            return Optional.of(DayOfWeek.THURSDAY);
        }
        if (containsAny(text, "пятницу", "пятница", "пятницы")) {
            return Optional.of(DayOfWeek.FRIDAY);
        }
        if (containsAny(text, "субботу", "суббота", "субботы")) {
            return Optional.of(DayOfWeek.SATURDAY);
        }
        if (containsAny(text, "воскресенье", "воскресенья")) {
            return Optional.of(DayOfWeek.SUNDAY);
        }
        return Optional.empty();
    }

    private Optional<LocalTime> extractTime(String text) {
        if (looksLikePartySizeAnswer(text) || looksLikeTableSelection(text) || containsAny(text, "стол", "столик")) {
            return Optional.empty();
        }
        Matcher matcher = TIME.matcher(text);
        return matcher.find() ? Optional.of(parseTime(matcher, text)) : Optional.empty();
    }

    private Optional<Integer> extractPartySize(String text) {
        if (text.contains("двоих") || text.contains("двоем") || text.contains("двух") || text.contains("двое") || text.contains("двоем")) {
            return Optional.of(2);
        }
        if (text.contains("троих") || text.contains("трое") || text.contains("трех")) {
            return Optional.of(3);
        }
        if (text.contains("четверых") || text.contains("четверо") || text.contains("четырех")) {
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

    private Optional<Integer> extractPartySize(String text, BotState currentState) {
        Optional<Integer> explicit = extractPartySize(text);
        if (explicit.isPresent()) {
            return explicit;
        }
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        if (state != BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE) {
            return Optional.empty();
        }
        Matcher shortAnswer = SHORT_PARTY_SIZE_ANSWER.matcher(text);
        if (!shortAnswer.matches()) {
            return Optional.empty();
        }
        int value = Integer.parseInt(shortAnswer.group(1));
        return value > 0 && value <= 20 ? Optional.of(value) : Optional.empty();
    }

    private boolean looksLikePartySizeAnswer(String text) {
        return extractPartySize(text).isPresent()
                && !containsAny(text, ":", "вечер", "утр", "дня", "ноч", "час", "ч ")
                && extractDate(text).isEmpty();
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
                || TABLE_NUMBER_IN_TEXT.matcher(text).matches()
                || TABLE_NUMBER_BEFORE_WORD.matcher(text).matches();
    }

    private LocalDate requestedDate(String text) {
        LocalDate today = timeProvider.today();
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
        Matcher reverseMatcher = Pattern.compile("(?:^|\\s)(1\\d|[1-9])\\s*стол(?:ик)?(?:\\s|$)").matcher(text);
        if (reverseMatcher.find()) {
            return reverseMatcher.group(1);
        }
        Matcher matcher = Pattern.compile("(?:^|\\s)(?:стол(?:ик)?\\s*)?(1\\d|[1-9])(?:\\s|$)").matcher(text);
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

    private Optional<String> seatingPreference(String text, boolean allowFreeText) {
        if (text == null || text.isBlank() || isNoSeatingPreference(text)) {
            return Optional.empty();
        }
        if (allowFreeText) {
            return Optional.of(text);
        }
        if (containsAny(text, "тих", "окн", "диван", "vip", "вип", "винн", "бар", "не проход", "уют")) {
            return Optional.of(text);
        }
        return Optional.empty();
    }

    private boolean isNoSeatingPreference(String text) {
        String normalized = normalize(text);
        return normalized.equals("нет")
                || normalized.equals("не")
                || normalized.equals("без")
                || normalized.equals("без пожеланий")
                || normalized.equals("пожеланий нет")
                || normalized.equals("нет пожеланий")
                || normalized.equals("любой")
                || normalized.equals("любой стол")
                || normalized.equals("как удобно")
                || normalized.equals("где удобно")
                || normalized.equals("на ваше усмотрение")
                || normalized.equals("на твой выбор")
                || normalized.equals("выбери сам");
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

    private Optional<TableBookingDraftStorage.Draft> findDraft(Long chatId) {
        Optional<TableBookingDraftStorage.Draft> draft = draftStorage.find(chatId);
        return draft == null ? Optional.empty() : draft;
    }

    private boolean containsAny(String text, String... variants) {
        for (String variant : variants) {
            if (text.contains(variant)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase().replace('ё', 'е');
    }
}
