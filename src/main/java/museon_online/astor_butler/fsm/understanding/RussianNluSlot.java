package museon_online.astor_butler.fsm.understanding;

public record RussianNluSlot(
        String name,
        String value,
        double confidence,
        String source
) {
}
