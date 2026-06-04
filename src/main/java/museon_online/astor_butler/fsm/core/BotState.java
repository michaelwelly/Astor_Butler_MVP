package museon_online.astor_butler.fsm.core;

public enum BotState {
    UNKNOWN,
    CONSENT_REQUIRED,
    READY_FOR_DIALOG,
    AI_FALLBACK,

    @Deprecated(forRemoval = false)
    GREETING,

    @Deprecated(forRemoval = false)
    CONTACT,

    @Deprecated(forRemoval = false)
    MENU;

    public BotState canonical() {
        return switch (this) {
            case GREETING, CONTACT -> CONSENT_REQUIRED;
            case MENU -> READY_FOR_DIALOG;
            default -> this;
        };
    }

    public boolean waitsForConsentAndContact() {
        return canonical() == CONSENT_REQUIRED;
    }

    public static BotState fromStorageValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return BotState.valueOf(value).canonical();
    }
}
