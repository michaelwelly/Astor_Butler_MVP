package museon_online.astor_butler.fsm.reply;

public record ScenarioReply(
        String text,
        boolean generated,
        boolean fallbackUsed,
        String provider,
        String model
) {

    public static ScenarioReply fallback(String text) {
        return new ScenarioReply(text, false, true, "", "");
    }
}
