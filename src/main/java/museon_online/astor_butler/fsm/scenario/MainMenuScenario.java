package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MainMenuScenario {

    private static final Pattern MONEY = Pattern.compile(".*\\b\\d{2,7}\\b.*");

    private final FSMStorage fsmStorage;

    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        if (isSafeExitIntent(normalized) && isActiveScenarioState(state)) {
            return true;
        }
        return state == BotState.READY_FOR_DIALOG || state == BotState.AI_FALLBACK
                ? detect(normalized) != MainMenuIntent.UNKNOWN
                : supportsContinuation(state, normalized);
    }

    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (isSafeExitIntent(normalized) && isActiveScenarioState(state)) {
            return ready(
                    incoming,
                    "Остановил текущий сценарий. Я на связи: стол, меню, афиша, мероприятие, чаевые, благотворительность или менеджер?",
                    "SAFE_EXIT",
                    "OPEN_MAIN_MENU"
            );
        }
        if (state == BotState.TIP_COLLECT_AMOUNT) {
            return smartTip(incoming, normalized);
        }
        if (state == BotState.TIP_CONFIRMATION) {
            return confirmSmartTip(incoming, normalized);
        }
        if (state == BotState.DONATION_COLLECT_AMOUNT) {
            return hiddenHeart(incoming, normalized);
        }
        if (state == BotState.DONATION_CONFIRMATION) {
            return confirmDonation(incoming, normalized);
        }
        if (state == BotState.AUCTION_WAIT_BID) {
            return continueAuction(incoming, normalized);
        }

        MainMenuIntent intent = detect(normalized);
        return switch (intent) {
            case MENU -> menu(incoming);
            case QUIET_GUIDE -> quietGuide(incoming);
            case EVENT_BOOKING -> eventBooking(incoming);
            case MANAGER_HELP -> managerHelp(incoming);
            case CHANGE_CANCEL -> changeCancel(incoming);
            case SMART_TIP -> smartTip(incoming, normalized);
            case HIDDEN_HEART -> hiddenHeart(incoming, normalized);
            case ART_AUCTION -> artAuction(incoming, normalized);
            case IMPACT -> impact(incoming);
            case UNKNOWN -> clarification(incoming);
        };
    }

    private OutgoingMessage menu(IncomingMessage incoming) {
        return ready(
                incoming,
                """
                Я на связи. Что сделаем?

                • Забронировать стол
                • Посмотреть меню или карту бара
                • Афиша и события
                • Организовать мероприятие
                • Чаевые / благодарность
                • Благотворительность / аукцион
                • Позвать менеджера
                """,
                "MAIN_MENU",
                "SHOW_MENU"
        );
    }

    private OutgoingMessage quietGuide(IncomingMessage incoming) {
        return ready(
                incoming,
                """
                По афише и материалам могу помочь без лишней рассылки.

                Сейчас в MVP я умею: меню, карту бара, план зала и передачу запроса менеджеру.
                Напишите: "меню", "карта бара", "план зала" или "менеджер".
                """,
                "QUIET_GUIDE",
                "CONTENT_LOOKUP"
        );
    }

    private OutgoingMessage eventBooking(IncomingMessage incoming) {
        return ready(
                incoming,
                """
                Для мероприятия соберу заявку для менеджера.

                Напишите одной фразой: дата, время, формат, количество гостей и пожелания. Например:
                "день рождения 20 июня в 19:00 на 25 гостей, нужен банкет".
                """,
                "EVENT_BOOKING_INTENT",
                "ASK_EVENT_DETAILS"
        );
    }

    private OutgoingMessage managerHelp(IncomingMessage incoming) {
        return ready(
                incoming,
                "Передам запрос команде. Напишите, пожалуйста, одним сообщением, что именно нужно менеджеру увидеть.",
                "MANAGER_HELP",
                "ASK_MANAGER_REASON"
        );
    }

    private OutgoingMessage changeCancel(IncomingMessage incoming) {
        return ready(
                incoming,
                "Понял, нужно изменить или отменить бронь. Напишите номер заявки, дату или время брони — найду активный запрос и подскажу следующий шаг.",
                "CHANGE_CANCEL",
                "ASK_ACTIVE_ORDER_REFERENCE"
        );
    }

    private OutgoingMessage smartTip(IncomingMessage incoming, String text) {
        if (!hasMoney(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.TIP_COLLECT_AMOUNT);
            return message(
                    incoming,
                    "Красивая благодарность. Какую сумму чаевых хотите оставить?",
                    BotState.TIP_COLLECT_AMOUNT,
                    "SMART_TIP",
                    "ASK_TIP_AMOUNT"
            );
        }
        fsmStorage.setState(incoming.chatId(), BotState.TIP_CONFIRMATION);
        return message(
                incoming,
                "Принял сумму чаевых. До платежного контура я сначала покажу подтверждение: кому благодарность и какая сумма. Подтверждаете?",
                BotState.TIP_CONFIRMATION,
                "SMART_TIP",
                "TIP_CONFIRMATION"
        );
    }

    private OutgoingMessage confirmSmartTip(IncomingMessage incoming, String text) {
        if (isConfirmIntent(text)) {
            return ready(
                    incoming,
                    "Зафиксировал draft благодарности. Следующий слой подключит платежный контур, а сейчас я вернул вас в главное меню.",
                    "SMART_TIP",
                    "TIP_DRAFT_CONFIRMED",
                    "RETURN_MAIN_MENU"
            );
        }
        if (isRejectIntent(text)) {
            return ready(
                    incoming,
                    "Хорошо, чаевые не фиксирую. Возвращаюсь в главное меню.",
                    "SMART_TIP",
                    "TIP_CANCELLED",
                    "RETURN_MAIN_MENU"
            );
        }
        return message(
                incoming,
                "Подтвердите, пожалуйста: да — зафиксировать draft чаевых, нет — отменить.",
                BotState.TIP_CONFIRMATION,
                "SMART_TIP",
                "ASK_CONFIRMATION"
        );
    }

    private OutgoingMessage hiddenHeart(IncomingMessage incoming, String text) {
        if (!hasMoney(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.DONATION_COLLECT_AMOUNT);
            return message(
                    incoming,
                    "Hidden Heart включен. Какую сумму хотите направить на благотворительный проект? По умолчанию донат будет анонимным.",
                    BotState.DONATION_COLLECT_AMOUNT,
                    "HIDDEN_HEART",
                    "ASK_DONATION_AMOUNT"
            );
        }
        fsmStorage.setState(incoming.chatId(), BotState.DONATION_CONFIRMATION);
        return message(
                incoming,
                "Собрал donation draft: анонимный вклад, сумма из сообщения. Перед платежным контуром я покажу подтверждение и не раскрою личные данные в impact-отчете.",
                BotState.DONATION_CONFIRMATION,
                "HIDDEN_HEART",
                "DONATION_CONFIRMATION",
                "IMPACT_EVENT_DRAFT"
        );
    }

    private OutgoingMessage confirmDonation(IncomingMessage incoming, String text) {
        if (isConfirmIntent(text)) {
            return ready(
                    incoming,
                    "Зафиксировал анонимный donation draft. В impact попадет только агрегированный вклад, без приватных данных.",
                    "HIDDEN_HEART",
                    "DONATION_DRAFT_CONFIRMED",
                    "IMPACT_EVENT_DRAFT",
                    "RETURN_MAIN_MENU"
            );
        }
        if (isRejectIntent(text)) {
            return ready(
                    incoming,
                    "Хорошо, донат не фиксирую. Возвращаюсь в главное меню.",
                    "HIDDEN_HEART",
                    "DONATION_CANCELLED",
                    "RETURN_MAIN_MENU"
            );
        }
        return message(
                incoming,
                "Подтвердите, пожалуйста: да — зафиксировать анонимный donation draft, нет — отменить.",
                BotState.DONATION_CONFIRMATION,
                "HIDDEN_HEART",
                "ASK_CONFIRMATION"
        );
    }

    private OutgoingMessage artAuction(IncomingMessage incoming, String text) {
        if (!hasMoney(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.AUCTION_WAIT_BID);
            return message(
                    incoming,
                    "Аукцион по картинам работает только при активном лоте события. Напишите сумму ставки или попросите менеджера показать текущий лот.",
                    BotState.AUCTION_WAIT_BID,
                    "ART_AUCTION",
                    "ASK_AUCTION_BID"
            );
        }
        fsmStorage.setState(incoming.chatId(), BotState.AUCTION_WAIT_BID);
        return message(
                incoming,
                "Ставку вижу. Перед тем как принять ее, проверю активный лот и минимальный шаг, затем попрошу явное подтверждение. LLM ставку сам не принимает.",
                BotState.AUCTION_WAIT_BID,
                "ART_AUCTION",
                "VALIDATE_AUCTION_BID",
                "ASK_EXPLICIT_CONFIRMATION"
        );
    }

    private OutgoingMessage continueAuction(IncomingMessage incoming, String text) {
        if (isConfirmIntent(text)) {
            return ready(
                    incoming,
                    "Принял подтверждение ставки как заявку к активному лоту. Финальный прием ставки требует проверки лота, минимального шага и подтверждения event owner.",
                    "ART_AUCTION",
                    "AUCTION_BID_GUEST_CONFIRMED",
                    "MANAGER_CONFIRMATION_REQUIRED",
                    "RETURN_MAIN_MENU"
            );
        }
        if (isRejectIntent(text)) {
            return ready(
                    incoming,
                    "Хорошо, ставку не фиксирую. Можно остаться наблюдателем или вернуться к другому сценарию.",
                    "ART_AUCTION",
                    "AUCTION_BID_CANCELLED",
                    "RETURN_MAIN_MENU"
            );
        }
        return artAuction(incoming, text);
    }

    private OutgoingMessage impact(IncomingMessage incoming) {
        return ready(
                incoming,
                "Impact Meter покажет только агрегированные итоги: донаты, аукционы, чаевые и культурный вклад без приватных платежных данных.",
                "IMPACT_METER",
                "SHOW_IMPACT_SUMMARY"
        );
    }

    private OutgoingMessage clarification(IncomingMessage incoming) {
        return ready(
                incoming,
                "Я пока не уверен, какой сценарий нужен. Выберите: стол, меню, афиша, мероприятие, чаевые, благотворительность или менеджер.",
                "MAIN_MENU_CLARIFY"
        );
    }

    private OutgoingMessage ready(IncomingMessage incoming, String text, String... actions) {
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return message(incoming, text, BotState.READY_FOR_DIALOG, actions);
    }

    private OutgoingMessage message(IncomingMessage incoming, String text, BotState nextState, String... actions) {
        return OutgoingMessage.of(
                incoming,
                text,
                nextState.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of(actions)
        );
    }

    private boolean supportsContinuation(BotState state, String text) {
        return switch (state) {
            case TIP_COLLECT_AMOUNT -> hasMoney(text) || isSafeExitIntent(text);
            case TIP_CONFIRMATION -> isConfirmIntent(text) || isRejectIntent(text) || isSafeExitIntent(text);
            case DONATION_COLLECT_AMOUNT -> hasMoney(text) || isSafeExitIntent(text);
            case DONATION_CONFIRMATION -> isConfirmIntent(text) || isRejectIntent(text) || isSafeExitIntent(text);
            case AUCTION_WAIT_BID -> hasMoney(text) || isConfirmIntent(text) || isRejectIntent(text) || isSafeExitIntent(text);
            default -> false;
        };
    }

    private MainMenuIntent detect(String text) {
        if (isMenuIntent(text)) {
            return MainMenuIntent.MENU;
        }
        if (containsAny(text, "афиша", "что сегодня", "расписание", "что будет", "что у вас", "постер")) {
            return MainMenuIntent.QUIET_GUIDE;
        }
        if (containsAny(text, "банкет", "день рождения", "корпоратив", "свадьба", "мероприятие", "выкуп зала")) {
            return MainMenuIntent.EVENT_BOOKING;
        }
        if (containsAny(text, "менеджер", "человек", "администратор", "жалоба", "позови")) {
            return MainMenuIntent.MANAGER_HELP;
        }
        if (containsAny(text, "отменить брон", "отмена брон", "перенести брон", "изменить брон", "не придем")) {
            return MainMenuIntent.CHANGE_CANCEL;
        }
        if (containsAny(text, "чаевые", "поблагодарить", "спасибо официанту", "благодарность")) {
            return MainMenuIntent.SMART_TIP;
        }
        if (containsAny(text, "донат", "благотвор", "поддержать проект", "hidden heart")) {
            return MainMenuIntent.HIDDEN_HEART;
        }
        if (containsAny(text, "аукцион", "картина", "ставка", "ставлю", "лот")) {
            return MainMenuIntent.ART_AUCTION;
        }
        if (containsAny(text, "impact", "итоги", "сколько собрали", "культурный вклад")) {
            return MainMenuIntent.IMPACT;
        }
        return MainMenuIntent.UNKNOWN;
    }

    private boolean isMenuIntent(String text) {
        return text.equals("/menu")
                || text.equals("меню")
                || text.contains("главное меню")
                || text.contains("карта бара")
                || text.contains("барная карта")
                || text.contains("еда")
                || text.contains("напитки");
    }

    private boolean isActiveScenarioState(BotState state) {
        return state != BotState.UNKNOWN
                && state != BotState.CONSENT_REQUIRED
                && state != BotState.READY_FOR_DIALOG
                && state != BotState.AI_FALLBACK;
    }

    private boolean isSafeExitIntent(String text) {
        return text.equals("стоп")
                || text.equals("назад")
                || text.equals("выйти")
                || text.equals("отмена")
                || text.equals("отмени")
                || text.equals("/cancel");
    }

    private boolean isConfirmIntent(String text) {
        return text.equals("да")
                || text.equals("ок")
                || text.equals("okay")
                || text.equals("ok")
                || text.equals("подтверждаю")
                || text.equals("подтвердить")
                || text.equals("согласен")
                || text.equals("согласна");
    }

    private boolean isRejectIntent(String text) {
        return text.equals("нет")
                || text.equals("не надо")
                || text.equals("отмена")
                || text.equals("отмени")
                || text.equals("не подтверждаю");
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMoney(String text) {
        return MONEY.matcher(text).matches()
                || text.contains("тысяч")
                || text.contains("тысячи")
                || text.contains("руб");
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private enum MainMenuIntent {
        MENU,
        QUIET_GUIDE,
        EVENT_BOOKING,
        MANAGER_HELP,
        CHANGE_CANCEL,
        SMART_TIP,
        HIDDEN_HEART,
        ART_AUCTION,
        IMPACT,
        UNKNOWN
    }
}
