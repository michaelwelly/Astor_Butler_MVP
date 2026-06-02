package museon_online.astor_butler.service.message;

public record AdminAlert(
        boolean required,
        String chatId,
        String text
) {
    public static AdminAlert none() {
        return new AdminAlert(false, null, null);
    }
}
