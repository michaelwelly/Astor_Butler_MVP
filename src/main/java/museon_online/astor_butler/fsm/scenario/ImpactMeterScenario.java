package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
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
public class ImpactMeterScenario implements FsmScenario {

    private final FSMStorage fsmStorage;

    @Override
    public String id() {
        return "IMPACT_METER";
    }

    @Override
    public int priority() {
        return 52;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        return (state == BotState.READY_FOR_DIALOG || state == BotState.AI_FALLBACK)
                && isImpactIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Impact Meter покажет только агрегированные итоги: донаты, аукционы, чаевые и культурный вклад без приватных платежных данных.",
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("IMPACT_METER", "SHOW_IMPACT_SUMMARY")
        ).withMetadata(Map.of(
                "scenario", id(),
                "privacy", "AGGREGATED_ONLY",
                "containsPrivatePaymentData", false
        ));
    }

    @Override
    public boolean canRunInParallel() {
        return true;
    }

    private boolean isImpactIntent(String text) {
        return containsAny(text, "impact", "итоги", "сколько собрали", "культурный вклад", "вклад проекта", "социальный вклад");
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
