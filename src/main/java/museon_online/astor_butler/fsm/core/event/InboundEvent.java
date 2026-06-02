package museon_online.astor_butler.fsm.core.event;

import org.telegram.telegrambots.meta.api.objects.Update;

public class InboundEvent {

    private final String eventId;
    private final Long chatId;
    private final EventType type;
    private final String payload;

    private InboundEvent(String eventId, Long chatId, EventType type, String payload) {
        this.eventId = eventId;
        this.chatId = chatId;
        this.type = type;
        this.payload = payload;
    }

    public static InboundEvent from(Update update) {
        if (update == null || update.getMessage() == null) {
            return null;
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        String eventId = String.valueOf(update.getUpdateId());

        if (update.getMessage().getContact() != null) {
            return new InboundEvent(
                    eventId,
                    chatId,
                    EventType.CONTACT,
                    update.getMessage().getContact().getPhoneNumber()
            );
        }

        if (text == null) {
            return new InboundEvent(
                    eventId,
                    chatId,
                    EventType.UNKNOWN,
                    ""
            );
        }

        return new InboundEvent(
                eventId,
                chatId,
                text.startsWith("/") ? EventType.COMMAND : EventType.TEXT,
                text
        );
    }

    public String getEventId() {
        return eventId;
    }

    public Long getChatId() {
        return chatId;
    }

    public EventType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }
}
