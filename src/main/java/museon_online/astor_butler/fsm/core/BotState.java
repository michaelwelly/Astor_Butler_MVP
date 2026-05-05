package museon_online.astor_butler.fsm.core;

public enum BotState {
    UNKNOWN,    // 👈 дефолтное, когда Redis пуст или пользователь неизвестен
    GREETING,   // приветствие
    CONTACT,    // получение контакта
    MENU,       // основное меню
    AI_FALLBACK, // резервная обработка (AI)

    EVENT_BOOKING_TYPE,
    EVENT_BOOKING_DATE,
    EVENT_BOOKING_GUEST_COUNT,
    EVENT_BOOKING_FORMAT,
    EVENT_BOOKING_BUDGET,
    EVENT_BOOKING_MENU,
    EVENT_BOOKING_TECHNICAL_REQUIREMENTS,
    EVENT_BOOKING_CONTACT,
    EVENT_BOOKING_SUMMARY,
    EVENT_BOOKING_READY_FOR_MANAGER,
    EVENT_BOOKING_ESCALATION
}
