package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.fsm.core.BotState;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TableBookingStepRegistry {

    public Optional<Step> nextMissingStep(TableBookingDraftStorage.Draft draft) {
        if (!hasTableSelection(draft)) {
            return Optional.of(new Step(
                    BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION,
                    "ASK_TABLE_SELECTION",
                    "Отправляю план зала AERIS. Выберите номер стола или зону: например, «18 стол», «винная комната», «у бара». Если хотите, я сам подберу подходящий вариант."
            ));
        }
        if (draft.requestedDate() == null) {
            return Optional.of(new Step(
                    BotState.TABLE_BOOKING_COLLECT_DATE,
                    "ASK_DATE",
                    "Отлично, стол отметил. На какой день поставить бронь?"
            ));
        }
        if (draft.requestedTime() == null) {
            return Optional.of(new Step(
                    BotState.TABLE_BOOKING_COLLECT_TIME,
                    "ASK_TIME",
                    "Принял дату. Во сколько вас ждать?"
            ));
        }
        if (draft.partySize() == null) {
            return Optional.of(new Step(
                    BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE,
                    "ASK_PARTY_SIZE",
                    "На сколько гостей подготовить стол?"
            ));
        }
        return Optional.empty();
    }

    public boolean hasTableSelection(TableBookingDraftStorage.Draft draft) {
        return draft != null
                && (hasText(draft.tableCode())
                || hasText(draft.preferredZone())
                || containsAny(normalize(draft.seatingPreference()), "выбери сам", "любой", "на твой выбор", "где удобно"));
    }

    public boolean hasSeatingPreferenceDecision(TableBookingDraftStorage.Draft draft) {
        return draft != null
                && (Boolean.TRUE.equals(draft.seatingPreferenceResolved()) || hasText(draft.seatingPreference()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
        return text == null ? "" : text.trim().toLowerCase();
    }

    public record Step(BotState state, String action, String deterministicPrompt) {
    }
}
