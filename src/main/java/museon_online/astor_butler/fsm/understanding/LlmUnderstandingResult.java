package museon_online.astor_butler.fsm.understanding;

import java.util.List;

public record LlmUnderstandingResult(
        InputIntent intent,
        double confidence,
        List<SlotValue> slots,
        List<String> missingSlots,
        String replyDraft,
        String provider,
        String model
) {
    public static LlmUnderstandingResult empty() {
        return new LlmUnderstandingResult(
                InputIntent.UNKNOWN,
                0.0,
                List.of(),
                List.of(),
                "",
                "",
                ""
        );
    }

    public boolean usable(double threshold) {
        return intent != null
                && intent != InputIntent.UNKNOWN
                && confidence >= threshold;
    }
}
