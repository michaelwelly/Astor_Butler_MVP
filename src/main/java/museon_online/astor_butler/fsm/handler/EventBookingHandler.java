package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.domain.booking.EventBookingDraft;
import museon_online.astor_butler.domain.booking.EventBookingDraftStorage;
import museon_online.astor_butler.domain.booking.EventBookingService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.telegram.utils.TelegramSender;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventBookingHandler implements FSMHandler {

    private static final List<BotState> STATES = List.of(
            BotState.EVENT_BOOKING_TYPE,
            BotState.EVENT_BOOKING_DATE,
            BotState.EVENT_BOOKING_GUEST_COUNT,
            BotState.EVENT_BOOKING_FORMAT,
            BotState.EVENT_BOOKING_BUDGET,
            BotState.EVENT_BOOKING_MENU,
            BotState.EVENT_BOOKING_TECHNICAL_REQUIREMENTS,
            BotState.EVENT_BOOKING_CONTACT,
            BotState.EVENT_BOOKING_SUMMARY,
            BotState.EVENT_BOOKING_READY_FOR_MANAGER,
            BotState.EVENT_BOOKING_ESCALATION
    );

    private final TelegramSender sender;
    private final FSMStorage storage;
    private final EventBookingDraftStorage draftStorage;
    private final EventBookingService bookingService;

    @Override
    public BotState getState() {
        return BotState.EVENT_BOOKING_TYPE;
    }

    @Override
    public List<BotState> getStates() {
        return STATES;
    }

    @Override
    public void handle(CommandContext ctx) {
        Long chatId = ctx.getChatId();
        if (chatId == null) {
            log.warn("EVENT_BOOKING received context without chatId");
            return;
        }

        String text = normalize(ctx.getMessageText());
        BotState state = storage.getState(chatId);
        if (state == null || !STATES.contains(state)) {
            state = BotState.EVENT_BOOKING_TYPE;
            storage.setState(chatId, state);
        }

        if (isCancel(text)) {
            draftStorage.clear(chatId);
            storage.setState(chatId, BotState.MENU);
            sender.sendText(chatId, "Заявку на мероприятие отменил. Можем начать заново, когда будет удобно.");
            return;
        }

        if (isStartCommand(text)) {
            draftStorage.clear(chatId);
            storage.setState(chatId, BotState.EVENT_BOOKING_TYPE);
            sender.sendText(chatId, """
                    Давайте соберем заявку на мероприятие.

                    Подскажите, пожалуйста, что планируете: банкет, фуршет, корпоратив, свадьбу, день рождения или другой формат?""");
            return;
        }

        EventBookingDraft draft = draftStorage.getOrCreate(chatId);

        switch (state) {
            case EVENT_BOOKING_TYPE -> collectEventType(chatId, text, draft);
            case EVENT_BOOKING_DATE -> collectEventDate(chatId, text, draft);
            case EVENT_BOOKING_GUEST_COUNT -> collectGuestCount(chatId, text, draft);
            case EVENT_BOOKING_FORMAT -> collectEventFormat(chatId, text, draft);
            case EVENT_BOOKING_BUDGET -> collectBudget(chatId, text, draft);
            case EVENT_BOOKING_MENU -> collectMenu(chatId, text, draft);
            case EVENT_BOOKING_TECHNICAL_REQUIREMENTS -> collectTechnicalRequirements(chatId, text, draft);
            case EVENT_BOOKING_CONTACT -> collectContact(chatId, text, draft);
            case EVENT_BOOKING_SUMMARY -> confirmSummary(chatId, text, draft);
            case EVENT_BOOKING_READY_FOR_MANAGER -> sendReadyForManager(chatId, draft);
            case EVENT_BOOKING_ESCALATION -> escalate(chatId, draft);
            default -> {
                storage.setState(chatId, BotState.EVENT_BOOKING_TYPE);
                askEventType(chatId);
            }
        }
    }

    private void collectEventType(Long chatId, String text, EventBookingDraft draft) {
        if (isBlank(text)) {
            askEventType(chatId);
            return;
        }
        draft.setEventType(text);
        saveAndMove(chatId, draft, BotState.EVENT_BOOKING_DATE);
        sender.sendText(chatId, "На какую дату и примерное время рассматриваете мероприятие?");
    }

    private void collectEventDate(Long chatId, String text, EventBookingDraft draft) {
        if (isBlank(text)) {
            sender.sendText(chatId, "Подскажите дату и примерное время. Доступность я не подтверждаю сам, передам менеджеру для проверки.");
            return;
        }
        draft.setEventDate(text);
        saveAndMove(chatId, draft, BotState.EVENT_BOOKING_GUEST_COUNT);
        sender.sendText(chatId, "Сколько гостей планируется?");
    }

    private void collectGuestCount(Long chatId, String text, EventBookingDraft draft) {
        if (isBlank(text)) {
            sender.sendText(chatId, "Сколько гостей ожидается? Можно примерный диапазон.");
            return;
        }
        draft.setGuestCount(text);
        saveAndMove(chatId, draft, BotState.EVENT_BOOKING_FORMAT);
        sender.sendText(chatId, "Какой формат нужен: банкет, фуршет, кофе-брейк, смешанный формат или что-то свое?");
    }

    private void collectEventFormat(Long chatId, String text, EventBookingDraft draft) {
        if (isBlank(text)) {
            sender.sendText(chatId, "Напишите формат мероприятия. Если пока не решили, так и скажите.");
            return;
        }
        draft.setEventFormat(text);
        saveAndMove(chatId, draft, BotState.EVENT_BOOKING_BUDGET);
        sender.sendText(chatId, "Есть ориентир по бюджету? Можно диапазон или ответить «пока не знаю».");
    }

    private void collectBudget(Long chatId, String text, EventBookingDraft draft) {
        draft.setBudget(defaultIfSkipped(text));
        saveAndMove(chatId, draft, BotState.EVENT_BOOKING_MENU);
        sender.sendText(chatId, "Есть пожелания по меню, напиткам или ограничениям по питанию?");
    }

    private void collectMenu(Long chatId, String text, EventBookingDraft draft) {
        draft.setMenuPreferences(defaultIfSkipped(text));
        saveAndMove(chatId, draft, BotState.EVENT_BOOKING_TECHNICAL_REQUIREMENTS);
        sender.sendText(chatId, "Нужны звук, свет, экран, ведущий, декор или другие подрядчики?");
    }

    private void collectTechnicalRequirements(Long chatId, String text, EventBookingDraft draft) {
        draft.setTechnicalRequirements(defaultIfSkipped(text));
        saveAndMove(chatId, draft, BotState.EVENT_BOOKING_CONTACT);
        sender.sendText(chatId, "Как менеджеру с вами связаться? Напишите имя и телефон или удобный Telegram.");
    }

    private void collectContact(Long chatId, String text, EventBookingDraft draft) {
        if (isBlank(text)) {
            sender.sendText(chatId, "Оставьте, пожалуйста, контакт для менеджера: имя и телефон или Telegram.");
            return;
        }
        draft.setContactDetails(text);
        saveAndMove(chatId, draft, BotState.EVENT_BOOKING_SUMMARY);
        sender.sendText(chatId, buildSummary(draft));
    }

    private void confirmSummary(Long chatId, String text, EventBookingDraft draft) {
        if (isConfirmation(text)) {
            sendReadyForManager(chatId, draft);
            return;
        }

        if (isManagerRequested(text)) {
            escalate(chatId, draft);
            return;
        }

        draft.setClientComment(append(draft.getClientComment(), text));
        draftStorage.save(chatId, draft);
        sender.sendText(chatId, """
                Я добавил это как комментарий к заявке.

                Если все верно, ответьте «да». Если нужен менеджер, напишите «менеджер».""");
    }

    private void sendReadyForManager(Long chatId, EventBookingDraft draft) {
        bookingService.saveReadyForManager(chatId, draft);
        draftStorage.clear(chatId);
        storage.setState(chatId, BotState.EVENT_BOOKING_READY_FOR_MANAGER);
        sender.sendText(chatId, """
                Заявка собрана и готова для менеджера.

                Я не подтверждаю бронь автоматически: менеджер проверит дату, условия площадки и вернется с подтверждением.""");
    }

    private void escalate(Long chatId, EventBookingDraft draft) {
        bookingService.saveManagerReview(chatId, draft);
        draftStorage.clear(chatId);
        storage.setState(chatId, BotState.EVENT_BOOKING_ESCALATION);
        sender.sendText(chatId, "Передам заявку менеджеру. Он проверит детали и продолжит общение с вами.");
    }

    private void askEventType(Long chatId) {
        sender.sendText(chatId, "Подскажите, пожалуйста, какое мероприятие планируете?");
    }

    private void saveAndMove(Long chatId, EventBookingDraft draft, BotState nextState) {
        draftStorage.save(chatId, draft);
        storage.setState(chatId, nextState);
    }

    private String buildSummary(EventBookingDraft draft) {
        return """
                Проверьте, пожалуйста, заявку:

                Мероприятие: %s
                Дата и время: %s
                Гостей: %s
                Формат: %s
                Бюджет: %s
                Меню/напитки: %s
                Техника/подрядчики: %s
                Контакт: %s

                Если все верно, ответьте «да». Если нужно что-то добавить, напишите одним сообщением."""
                .formatted(
                        value(draft.getEventType()),
                        value(draft.getEventDate()),
                        value(draft.getGuestCount()),
                        value(draft.getEventFormat()),
                        value(draft.getBudget()),
                        value(draft.getMenuPreferences()),
                        value(draft.getTechnicalRequirements()),
                        value(draft.getContactDetails())
                );
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private boolean isStartCommand(String text) {
        return "/event_booking".equalsIgnoreCase(text)
                || "/table_booking".equalsIgnoreCase(text)
                || "бронь мероприятия".equalsIgnoreCase(text);
    }

    private boolean isCancel(String text) {
        return "/cancel".equalsIgnoreCase(text)
                || "отмена".equalsIgnoreCase(text)
                || "отменить".equalsIgnoreCase(text);
    }

    private boolean isConfirmation(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.equals("да")
                || normalized.equals("ок")
                || normalized.equals("верно")
                || normalized.equals("подтверждаю")
                || normalized.equals("все верно")
                || normalized.equals("всё верно");
    }

    private boolean isManagerRequested(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("менеджер")
                || normalized.contains("человек")
                || normalized.contains("оператор");
    }

    private String defaultIfSkipped(String text) {
        if (isBlank(text) || "/skip".equalsIgnoreCase(text) || "пропустить".equalsIgnoreCase(text)) {
            return "Уточнить позже";
        }
        return text;
    }

    private String value(String text) {
        return isBlank(text) ? "Уточнить позже" : text;
    }

    private String append(String previous, String next) {
        if (isBlank(next)) {
            return previous;
        }
        if (isBlank(previous)) {
            return next;
        }
        return previous + "\n" + next;
    }
}
