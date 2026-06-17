package museon_online.astor_butler.fsm.understanding;

import museon_online.astor_butler.domain.semantic.EmbeddingProvider;
import museon_online.astor_butler.domain.semantic.IntentExampleMatch;
import museon_online.astor_butler.domain.semantic.IntentExampleRepository;
import museon_online.astor_butler.fsm.core.BotState;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GuestInputUnderstandingService {

    private static final Pattern COMPACT_PARTY_SIZE = Pattern.compile("(?:^|\\s)на\\s+(\\d{1,2})\\s*(?:x|х|-х|-x)(?:\\s|$)");
    private static final Pattern GUEST_PARTY_SIZE = Pattern.compile("(?:^|\\s)(\\d{1,2})\\s*(?:гостей|гостя|человек|персон|чел)");
    private static final Pattern EVENING_TIME = Pattern.compile("(?:^|\\s)(?:в\\s*)?([1-9]|1[0-1])\\s*(?:вечера|вечер)(?:\\s|$)");
    private static final Pattern HOUR_TIME = Pattern.compile("(?:^|\\s)(?:в|к)?\\s*([01]?\\d|2[0-3])\\s*(?:час(?:ов|а)?|ч)(?:\\s|$)");
    private static final Pattern AROUND_HOUR_TIME = Pattern.compile("(?:^|\\s)(?:к|около|примерно|часам к)\\s*([1-9]|1[0-1])(?:\\s|$)");
    private static final Pattern EXPLICIT_TIME = Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b");
    private static final Pattern TABLE_NUMBER = Pattern.compile("^(?:стол(?:ик)?\\s*)?(1\\d|[1-9])$");

    private final IntentExampleRepository intentExampleRepository;
    private final EmbeddingProvider embeddingProvider;

    public GuestInputUnderstandingService() {
        this.intentExampleRepository = null;
        this.embeddingProvider = null;
    }

    @Autowired
    public GuestInputUnderstandingService(
            ObjectProvider<IntentExampleRepository> intentExampleRepository,
            ObjectProvider<EmbeddingProvider> embeddingProvider
    ) {
        this.intentExampleRepository = intentExampleRepository.getIfAvailable();
        this.embeddingProvider = embeddingProvider.getIfAvailable();
    }

    public UnderstoodInput understand(String rawText, BotState currentState) {
        String raw = rawText == null ? "" : rawText.trim();
        String normalized = normalize(raw);
        Map<String, SlotValue> slots = new LinkedHashMap<>();

        normalized = canonicalMenuPrompt(normalized);
        normalized = normalizeDateShortcut(normalized, slots);
        normalized = normalizeTime(normalized, slots);
        normalized = normalizePartySize(normalized, slots);
        captureTableSelection(normalized, slots);

        InputIntent primary = detectIntent(normalized, currentState, slots);
        List<InputIntent> candidates = detectCandidates(normalized, primary, slots);
        double confidence = confidence(primary, slots);
        IntentExampleMatch semanticMatch = findSemanticMatch(normalized, currentState, confidence).orElse(null);
        if (semanticMatch != null) {
            InputIntent matchedIntent = parseIntent(semanticMatch.intent()).orElse(primary);
            if (matchedIntent != primary) {
                candidates = prependCandidate(candidates, primary);
            }
            primary = matchedIntent;
            confidence = Math.max(confidence, Math.min(0.94, semanticMatch.score()));
            candidates = prependCandidate(candidates, matchedIntent);
        }

        return new UnderstoodInput(
                raw,
                normalized,
                primary,
                confidence,
                Map.copyOf(slots),
                List.copyOf(candidates),
                confidence < 0.55,
                confidence < 0.55 ? "Уточните, пожалуйста: бронь, меню, афиша, видео-тур или помощь команды?" : null
        );
    }

    private Optional<IntentExampleMatch> findSemanticMatch(String normalized, BotState currentState, double currentConfidence) {
        if (intentExampleRepository == null || normalized == null || normalized.isBlank() || currentConfidence >= 0.72) {
            return Optional.empty();
        }
        String state = currentState == null ? null : currentState.canonical().name();
        Optional<IntentExampleMatch> lexical = intentExampleRepository.findBestLexicalMatch("AERIS", state, normalized)
                .filter(match -> match.score() >= 0.58);
        if (lexical.isPresent()) {
            return lexical;
        }
        if (embeddingProvider == null) {
            return Optional.empty();
        }
        List<Double> embedding = embeddingProvider.embed(normalized);
        return intentExampleRepository.findNearestByEmbedding("AERIS", state, embedding, 3).stream()
                .filter(match -> match.score() >= 0.68)
                .findFirst();
    }

    private Optional<InputIntent> parseIntent(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(InputIntent.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private List<InputIntent> prependCandidate(List<InputIntent> candidates, InputIntent intent) {
        List<InputIntent> result = new ArrayList<>();
        result.add(intent);
        for (InputIntent candidate : candidates) {
            if (candidate != intent) {
                result.add(candidate);
            }
        }
        return result;
    }

    private String canonicalMenuPrompt(String normalized) {
        return switch (normalized) {
            case "📅 забронировать стол" -> "забронировать стол";
            case "📖 меню и карты" -> "покажи меню";
            case "🥂 сабраж" -> "сабраж";
            case "🏛 видео-тур" -> "покажи видео-тур";
            case "🎟 афиша" -> "покажи афишу";
            case "✨ концепция" -> "расскажи про концепцию aeris";
            case "🎉 мероприятие" -> "организовать мероприятие";
            case "🛎 помощь команды" -> "помощь команды";
            case "✏️ изменить / отменить" -> "изменить или отменить бронь";
            case "💬 оставить отзыв" -> "оставить отзыв";
            case "💚 чаевые" -> "чаевые";
            case "🤍 донат" -> "донат";
            case "🎨 аукцион" -> "аукцион";
            case "🎁 мерч" -> "мерч";
            case "🏠 главное меню" -> "главное меню";
            default -> normalized;
        };
    }

    private String normalizeDateShortcut(String text, Map<String, SlotValue> slots) {
        if (text.contains("послезавтра")) {
            slots.put("date", new SlotValue("date", "послезавтра", 0.95));
            return text;
        }
        if (text.contains("завтра")) {
            slots.put("date", new SlotValue("date", "завтра", 0.95));
            return text.replace("на завтра", "завтра");
        }
        if (text.contains("сегодня")) {
            slots.put("date", new SlotValue("date", "сегодня", 0.95));
        }
        return text;
    }

    private String normalizeTime(String text, Map<String, SlotValue> slots) {
        text = normalizeTimeWords(text);
        Matcher explicit = EXPLICIT_TIME.matcher(text);
        if (explicit.find()) {
            slots.put("time", new SlotValue("time", explicit.group(), 0.98));
            return text;
        }

        Matcher evening = EVENING_TIME.matcher(text);
        if (evening.find()) {
            int hour = Integer.parseInt(evening.group(1));
            return replaceTime(text, slots, evening, hour + 12);
        }

        Matcher around = AROUND_HOUR_TIME.matcher(text);
        if (around.find()) {
            int hour = Integer.parseInt(around.group(1));
            return replaceTime(text, slots, around, hour + 12);
        }

        Matcher hour = HOUR_TIME.matcher(text);
        if (hour.find()) {
            int parsedHour = Integer.parseInt(hour.group(1));
            return replaceTime(text, slots, hour, parsedHour);
        }
        return text;
    }

    private String normalizeTimeWords(String text) {
        return text
                .replace("шести", "6")
                .replace("семи", "7")
                .replace("восьми", "8")
                .replace("девяти", "9")
                .replace("десяти", "10")
                .replace("одиннадцати", "11");
    }

    private String replaceTime(String text, Map<String, SlotValue> slots, Matcher matcher, int hour) {
        LocalTime time = LocalTime.of(hour, 0);
        String value = "%02d:00".formatted(time.getHour());
        slots.put("time", new SlotValue("time", value, 0.94));
        return matcher.replaceFirst(" " + value + " ").replaceAll("\\s+", " ").trim();
    }

    private String normalizePartySize(String text, Map<String, SlotValue> slots) {
        Integer partySize = partySizeFromWords(text);
        if (partySize != null) {
            slots.put("partySize", new SlotValue("partySize", partySize.toString(), 0.95));
            return replacePartySizeWords(text, partySize);
        }

        Matcher compact = COMPACT_PARTY_SIZE.matcher(text);
        if (compact.find()) {
            String value = compact.group(1);
            slots.put("partySize", new SlotValue("partySize", value, 0.94));
            return compact.replaceFirst(" на " + value + " гостей ");
        }

        Matcher guests = GUEST_PARTY_SIZE.matcher(text);
        if (guests.find()) {
            slots.put("partySize", new SlotValue("partySize", guests.group(1), 0.94));
        }
        return text;
    }

    private Integer partySizeFromWords(String text) {
        if (containsAny(text, "двоих", "двоем", "двоём", "двое", "двух")) {
            return 2;
        }
        if (containsAny(text, "троих", "трое", "трех", "трёх")) {
            return 3;
        }
        if (containsAny(text, "четверых", "четверо", "четырех", "четырёх")) {
            return 4;
        }
        if (containsAny(text, "вдвоем", "вдвоём", "будем вдвоем", "будем вдвоём", "нас двое", "мы вдвоем", "мы вдвоём")) {
            return 2;
        }
        if (containsAny(text, "втроем", "втроём", "нас трое", "мы втроем", "мы втроём")) {
            return 3;
        }
        if (containsAny(text, "вчетвером", "нас четверо", "мы вчетвером")) {
            return 4;
        }
        return null;
    }

    private String replacePartySizeWords(String text, int partySize) {
        String result = text;
        if (partySize == 2) {
            result = replaceVariants(result, "на 2 гостей", "на двоих", "двоих", "на двоем", "двоем", "на двоём", "двоём", "двое", "двух", "вдвоем", "вдвоём", "будем вдвоем", "будем вдвоём", "нас двое", "мы вдвоем", "мы вдвоём");
        }
        if (partySize == 3) {
            result = replaceVariants(result, "на 3 гостей", "на троих", "троих", "трое", "трех", "трёх", "втроем", "втроём", "нас трое", "мы втроем", "мы втроём");
        }
        if (partySize == 4) {
            result = replaceVariants(result, "на 4 гостей", "на четверых", "четверых", "четверо", "четырех", "четырёх", "вчетвером", "нас четверо", "мы вчетвером");
        }
        return result.replace("гостей гостей", "гостей");
    }

    private String replaceVariants(String text, String replacement, String... variants) {
        String result = text;
        for (String variant : variants) {
            result = result.replace(variant, replacement);
        }
        return result;
    }

    private void captureTableSelection(String text, Map<String, SlotValue> slots) {
        Matcher matcher = TABLE_NUMBER.matcher(text);
        if (matcher.matches()) {
            slots.put("tableNumber", new SlotValue("tableNumber", matcher.group(1), 0.9));
        }
        if (containsAny(text, "винн", "vip", "вип", "бар", "у окна", "окн", "тих", "диван", "не проход", "уют", "выбери сам", "любой")) {
            slots.put("seatingPreference", new SlotValue("seatingPreference", text, 0.78));
        }
    }

    private InputIntent detectIntent(String text, BotState currentState, Map<String, SlotValue> slots) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        if (state == BotState.TABLE_BOOKING_COLLECT_DATE && slots.containsKey("date")) {
            return InputIntent.PROVIDE_DATE;
        }
        if (state == BotState.TABLE_BOOKING_COLLECT_TIME && slots.containsKey("time")) {
            return InputIntent.PROVIDE_TIME;
        }
        if (state == BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE && slots.containsKey("partySize")) {
            return InputIntent.PROVIDE_PARTY_SIZE;
        }
        if (state == BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION
                && (slots.containsKey("tableNumber") || slots.containsKey("seatingPreference") || containsAny(text, "выбери сам", "любой"))) {
            return InputIntent.PROVIDE_TABLE_SELECTION;
        }
        if (containsAny(text, "забронировать стол", "забронировать столик", "бронь стол", "столик", "стол на", "есть места", "нужен стол", "нужен столик", "посадите", "место на")) {
            return InputIntent.TABLE_BOOKING;
        }
        if (containsAny(text, "меню", "винн", "барная карта", "коктей", "поесть")) {
            return InputIntent.MENU_ASSETS;
        }
        if (containsAny(text, "видео-тур", "видеотур", "покажи ресторан", "интерьер", "афиша", "концепц", "шеф")) {
            return InputIntent.QUIET_GUIDE;
        }
        if (containsAny(text, "сабраж", "шампан", "игрист")) {
            return InputIntent.SAFE_PLAY;
        }
        if (containsAny(text, "мероприят", "день рождения", "корпоратив", "банкет", "свадьб")) {
            return InputIntent.EVENT_BOOKING;
        }
        if (containsAny(text, "менеджер", "админ", "помощь команды", "позови человека")) {
            return InputIntent.MANAGER_HELP;
        }
        if (containsAny(text, "отзыв", "жалоб", "похвал")) {
            return InputIntent.FEEDBACK;
        }
        if (containsAny(text, "изменить", "отменить", "перенести")) {
            return InputIntent.CHANGE_CANCEL;
        }
        if (containsAny(text, "чаевые", "на чай", "поблагодарить официанта")) {
            return InputIntent.SMART_TIP;
        }
        if (containsAny(text, "донат", "благотвор", "поддержать проект")) {
            return InputIntent.HIDDEN_HEART;
        }
        if (containsAny(text, "аукцион", "картина", "ставка", "ставлю")) {
            return InputIntent.ART_AUCTION;
        }
        if (containsAny(text, "мерч", "цепь", "купить")) {
            return InputIntent.MERCH;
        }
        if (containsAny(text, "главное меню", "домой", "назад")) {
            return InputIntent.MAIN_MENU;
        }
        if (containsAny(text, "да", "ок", "окей", "подтверждаю")) {
            return InputIntent.AFFIRMATION;
        }
        if (containsAny(text, "нет", "не надо", "отмена")) {
            return InputIntent.NEGATION;
        }
        return InputIntent.UNKNOWN;
    }

    private List<InputIntent> detectCandidates(String text, InputIntent primary, Map<String, SlotValue> slots) {
        List<InputIntent> candidates = new ArrayList<>();
        candidates.add(primary);
        if (primary != InputIntent.TABLE_BOOKING && (slots.containsKey("date") || slots.containsKey("time") || slots.containsKey("partySize"))) {
            candidates.add(InputIntent.TABLE_BOOKING);
        }
        if (primary != InputIntent.MENU_ASSETS && containsAny(text, "меню", "винн", "бар", "коктей")) {
            candidates.add(InputIntent.MENU_ASSETS);
        }
        if (primary != InputIntent.QUIET_GUIDE && containsAny(text, "афиша", "концепц", "видео", "интерьер")) {
            candidates.add(InputIntent.QUIET_GUIDE);
        }
        return candidates;
    }

    private double confidence(InputIntent primary, Map<String, SlotValue> slots) {
        if (primary == InputIntent.UNKNOWN) {
            return 0.25;
        }
        if (!slots.isEmpty()) {
            return 0.92;
        }
        return switch (primary) {
            case TABLE_BOOKING, MENU_ASSETS, QUIET_GUIDE, SAFE_PLAY, EVENT_BOOKING, MANAGER_HELP -> 0.86;
            default -> 0.74;
        };
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
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("\\s+", " ");
    }
}
