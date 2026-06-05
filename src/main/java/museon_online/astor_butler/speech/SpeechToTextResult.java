package museon_online.astor_butler.speech;

import java.util.Map;

public record SpeechToTextResult(
        boolean available,
        boolean transcribed,
        String text,
        String reason,
        Map<String, Object> metadata
) {
    public static SpeechToTextResult unavailable(String reason) {
        return new SpeechToTextResult(false, false, "", reason, Map.of());
    }

    public static SpeechToTextResult failed(String reason, Map<String, Object> metadata) {
        return new SpeechToTextResult(true, false, "", reason, metadata == null ? Map.of() : Map.copyOf(metadata));
    }

    public static SpeechToTextResult transcribed(String text, Map<String, Object> metadata) {
        return new SpeechToTextResult(true, true, text == null ? "" : text.trim(), "", metadata == null ? Map.of() : Map.copyOf(metadata));
    }
}
