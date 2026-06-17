package museon_online.astor_butler.service.message;

import java.util.List;

public record AdminAlert(
        boolean required,
        String chatId,
        String text,
        List<List<Button>> buttons
) {
    public AdminAlert(boolean required, String chatId, String text) {
        this(required, chatId, text, List.of());
    }

    public static AdminAlert none() {
        return new AdminAlert(false, null, null, List.of());
    }

    public record Button(String text, String callbackData) {
    }
}
