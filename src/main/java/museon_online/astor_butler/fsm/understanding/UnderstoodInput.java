package museon_online.astor_butler.fsm.understanding;

import java.util.List;
import java.util.Map;

public record UnderstoodInput(
        String rawText,
        String normalizedText,
        InputIntent primaryIntent,
        double confidence,
        Map<String, SlotValue> slots,
        List<InputIntent> candidates,
        boolean needsClarification,
        String clarificationQuestion
) {
    public String routeText() {
        return normalizedText == null || normalizedText.isBlank() ? rawText : normalizedText;
    }
}
