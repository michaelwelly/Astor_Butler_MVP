package museon_online.astor_butler.domain.booking;

import org.springframework.stereotype.Component;

@Component
public class EventBookingSummaryFormatter {

    public String formatForManager(EventBooking booking) {
        return """
                Новая заявка на мероприятие

                ID: %s
                Статус: %s
                Chat ID: %s

                Мероприятие: %s
                Дата и время: %s
                Гостей: %s
                Формат: %s
                Бюджет: %s
                Меню/напитки: %s
                Техника/подрядчики: %s
                Контакт: %s
                Комментарий клиента: %s
                """
                .formatted(
                        value(booking.getId()),
                        value(booking.getStatus()),
                        value(booking.getChatId()),
                        value(booking.getEventType()),
                        value(booking.getEventDate()),
                        value(booking.getGuestCount()),
                        value(booking.getEventFormat()),
                        value(booking.getBudget()),
                        value(booking.getMenuPreferences()),
                        value(booking.getTechnicalRequirements()),
                        value(booking.getContactDetails()),
                        value(booking.getClientComment())
                );
    }

    private String value(Object value) {
        if (value == null) {
            return "Уточнить позже";
        }
        String text = value.toString();
        return text.isBlank() ? "Уточнить позже" : text;
    }
}
