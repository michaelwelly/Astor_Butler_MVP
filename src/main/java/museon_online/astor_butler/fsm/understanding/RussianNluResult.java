package museon_online.astor_butler.fsm.understanding;

import java.util.List;

public record RussianNluResult(
        String source,
        List<RussianNluSlot> slots
) {

    public static RussianNluResult empty(String source) {
        return new RussianNluResult(source, List.of());
    }
}
