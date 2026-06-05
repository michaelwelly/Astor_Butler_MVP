package museon_online.astor_butler.fsm.core;

public enum BotState {
    UNKNOWN,
    CONSENT_REQUIRED,
    READY_FOR_DIALOG,
    AI_FALLBACK,
    TABLE_BOOKING_INTENT,
    TABLE_BOOKING_COLLECT_DATE,
    TABLE_BOOKING_COLLECT_TIME,
    TABLE_BOOKING_COLLECT_PARTY_SIZE,
    TABLE_BOOKING_SHOW_PLAN,
    TABLE_BOOKING_WAIT_TABLE_SELECTION,
    TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION,
    TABLE_BOOKING_CONFIRMED,
    TABLE_BOOKING_REJECTED,
    TABLE_BOOKING_CHANGE_REQUESTED,
    TABLE_BOOKING_CANCELLED,

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
