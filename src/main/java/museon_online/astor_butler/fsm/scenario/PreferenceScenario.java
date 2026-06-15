package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.preference.GuestPreference;
import museon_online.astor_butler.domain.preference.GuestPreferenceCategory;
import museon_online.astor_butler.domain.preference.GuestPreferenceCommand;
import museon_online.astor_butler.domain.preference.GuestPreferenceService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PreferenceScenario implements FsmScenario {

    private final FSMStorage fsmStorage;
    private final GuestPreferenceService preferenceService;

    @Override
    public String id() {
        return "PREFERENCE_MAP";
    }

    @Override
    public int priority() {
        return 35;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return owns(state) || isPreferenceIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (state == BotState.PREFERENCE_COLLECT_TEXT) {
            return savePreference(incoming, currentState, text, "PREFERENCE_TEXT_RECEIVED");
        }
        if (isShortPreferenceCall(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.PREFERENCE_COLLECT_TEXT);
            return OutgoingMessage.of(
                    incoming,
                    "Что запомнить для следующих визитов? Например: не ем острое, люблю тихий стол, предпочитаю красное сухое.",
                    BotState.PREFERENCE_COLLECT_TEXT.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("PREFERENCE_MAP", "ASK_PREFERENCE_TEXT")
            ).withMetadata(Map.of("scenario", id()));
        }
        return savePreference(incoming, currentState, text, "PREFERENCE_DIRECT_TEXT");
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.PREFERENCE_COLLECT_TEXT || canonical == BotState.PREFERENCE_SAVED;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage savePreference(
            IncomingMessage incoming,
            BotState previousState,
            String text,
            String reasonAction
    ) {
        String preferenceText = stripPreferencePrefix(text);
        if (preferenceText.isBlank()) {
            fsmStorage.setState(incoming.chatId(), BotState.PREFERENCE_COLLECT_TEXT);
            return OutgoingMessage.of(
                    incoming,
                    "Что именно запомнить? Можно написать одной фразой: не ем острое, люблю тихий стол, предпочитаю красное сухое.",
                    BotState.PREFERENCE_COLLECT_TEXT.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("PREFERENCE_MAP", "ASK_PREFERENCE_TEXT")
            ).withMetadata(Map.of("scenario", id()));
        }
        GuestPreferenceCategory category = preferenceService.classify(preferenceText);
        GuestPreference preference = preferenceService.createPreference(new GuestPreferenceCommand(
                incoming.chatId(),
                incoming.telegramUserId(),
                null,
                "AERIS",
                category,
                preferenceText,
                previousState == null ? null : previousState.name(),
                incoming.correlationId(),
                "{}"
        ));
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Запомнил: %s\n\nБуду учитывать это как предпочтение гостя. Я снова в главном меню."
                        .formatted(preference.preferenceText()),
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("PREFERENCE_MAP", reasonAction, "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "preferenceId", preference.id(),
                "category", preference.category().name(),
                "preferenceBoundary", "GUEST_PROVIDED_ONLY"
        ));
    }

    private boolean isPreferenceIntent(String text) {
        return containsAny(text,
                "запомни", "запиши", "сохрани",
                "я предпочитаю", "мне нравится", "мне не нравится",
                "я люблю", "я не люблю", "я не ем", "не ем",
                "у меня аллерг", "аллергия", "предпочтение", "preferences"
        );
    }

    private boolean isShortPreferenceCall(String text) {
        return text.equals("предпочтения")
                || text.equals("мои предпочтения")
                || text.equals("запомнить")
                || text.equals("/preferences")
                || text.equals("preferences");
    }

    private String stripPreferencePrefix(String text) {
        String normalized = text == null ? "" : text.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String prefix : List.of("запомни,", "запомни", "запиши,", "запиши", "сохрани,", "сохрани")) {
            if (lower.startsWith(prefix)) {
                return normalized.substring(prefix.length()).trim();
            }
        }
        return normalized;
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
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }
}
