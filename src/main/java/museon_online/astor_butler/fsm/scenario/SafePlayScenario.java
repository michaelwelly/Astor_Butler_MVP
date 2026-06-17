package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.booking.TableReservationOrder;
import museon_online.astor_butler.domain.booking.TableReservationRepository;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SafePlayScenario implements FsmScenario {

    private final FSMStorage fsmStorage;
    private final TableReservationRepository tableReservationRepository;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    @Override
    public String id() {
        return "SAFE_PLAY";
    }

    @Override
    public int priority() {
        return 37;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        if (!owns(state) && isMerchChainPurchaseIntent(normalized)) {
            return false;
        }
        return owns(state) || isSafePlayIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (isDangerousHowTo(normalized)) {
            return refuseDangerousHowTo(incoming, currentState, text);
        }
        if (state == BotState.SAFE_PLAY_COLLECT_DETAILS) {
            return sendTeamRequest(incoming, currentState, text, "SAFE_PLAY_DETAILS_RECEIVED");
        }
        if (isShortSafePlayCall(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.SAFE_PLAY_COLLECT_DETAILS);
            return OutgoingMessage.of(
                    incoming,
                    "Могу передать команде запрос на безопасный сабражный ритуал. Напишите, пожалуйста: стол/время, игристое или шампанское, и хотите ли фото/видео-память момента.",
                    BotState.SAFE_PLAY_COLLECT_DETAILS.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("SAFE_PLAY", "ASK_SAFE_PLAY_DETAILS", "NO_DANGEROUS_HOW_TO")
            ).withMetadata(Map.of("scenario", id()));
        }
        return sendTeamRequest(incoming, currentState, text, "SAFE_PLAY_DIRECT_REQUEST");
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.SAFE_PLAY_COLLECT_DETAILS
                || canonical == BotState.SAFE_PLAY_WAIT_TEAM_CONFIRMATION;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage refuseDangerousHowTo(IncomingMessage incoming, BotState previousState, String text) {
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Я не даю инструкции по опасным трюкам. Могу предложить безопасный сабражный ритуал в AERIS: его выполняет обученная команда, а вы участвуете как гость опыта.",
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                adminAlert(incoming, previousState, text, "SAFE_PLAY_SAFETY_REFUSAL"),
                List.of("SAFE_PLAY", "NO_DANGEROUS_HOW_TO", "OFFER_TEAM_RITUAL", "ADMIN_ALERT", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "safetyBoundary", "NO_DANGEROUS_HOW_TO"
        ));
    }

    private OutgoingMessage sendTeamRequest(
            IncomingMessage incoming,
            BotState previousState,
            String text,
            String reasonAction
    ) {
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Передал запрос команде AERIS. Сабражный ритуал возможен только после проверки staff availability, бутылки, времени и безопасности площадки.",
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                adminAlert(incoming, previousState, text, reasonAction),
                List.of("SAFE_PLAY", reasonAction, "TEAM_CONFIRMATION_REQUIRED", "NO_DANGEROUS_HOW_TO", "ADMIN_ALERT", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "safetyBoundary", "TEAM_PERFORMS_RITUAL",
                "merchLink", "SABRAGE_CHAIN_OPTIONAL"
        ));
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState previousState, String text, String action) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }
        String body = """
                <b>Astor Butler / safe play</b>
                Гость запросил сабражный ритуал или игровой hospitality-момент

                <b>Гость</b>
                %s
                chat %s / user %s%s

                <b>Запрос</b>
                <blockquote>%s</blockquote>

                <b>Safety boundary</b>
                Не давать гостю dangerous how-to. Ритуал выполняет только обученная команда после проверки условий.

                <b>Контекст</b>
                Previous state: %s
                Scenario: SAFE_PLAY
                Action: %s
                %s

                <b>Действие</b>
                Проверьте стол/время, бутылку, staff availability и возможность ритуала. Нажмите Да/Нет ниже. При интересе к цепи передайте в merch flow.

                <b>Техника</b>
                Channel: %s
                Correlation: %s
                """.formatted(
                html(displayName(incoming)),
                html(text(incoming.chatId())),
                html(text(incoming.telegramUserId())),
                incoming.username() == null || incoming.username().isBlank() ? "" : " / @" + html(incoming.username()),
                html(blankAsEmptyLabel(text)),
                html(text(previousState)),
                html(action),
                html(bookingContext(incoming)),
                html(text(incoming.channel())),
                html(blankAsEmptyLabel(incoming.correlationId()))
        );
        return new AdminAlert(true, adminChatId, body, List.of(List.of(
                new AdminAlert.Button("Да, можно", "safe_play:approve:" + incoming.chatId()),
                new AdminAlert.Button("Нет", "safe_play:reject:" + incoming.chatId())
        )));
    }

    private String bookingContext(IncomingMessage incoming) {
        if (incoming == null || incoming.chatId() == null) {
            return "Активная бронь: не найдена";
        }
        return tableReservationRepository.findActiveOrdersByChatId(incoming.chatId())
                .stream()
                .findFirst()
                .map(this::bookingContext)
                .orElse("Активная бронь: не найдена. Если это предзаказ, уточните стол/время у гостя.");
    }

    private String bookingContext(TableReservationOrder order) {
        return """
                Активная бронь: #%s
                Стол: %s
                Время: %s - %s
                Гостей: %s
                Пожелание: %s
                Телефон: %s
                """.formatted(
                text(order.id()),
                tableName(order),
                text(order.requestedStartAt()),
                text(order.requestedEndAt()),
                text(order.partySize()),
                blankAsEmptyLabel(order.seatingPreference()),
                blankAsEmptyLabel(order.guestPhone())
        ).strip();
    }

    private String tableName(TableReservationOrder order) {
        if (order.tableCode() == null || order.tableCode().isBlank()) {
            return "не выбран";
        }
        if (order.tableDisplayName() == null || order.tableDisplayName().isBlank()) {
            return order.tableCode();
        }
        return order.tableDisplayName() + " (" + order.tableCode() + ")";
    }

    private boolean isSafePlayIntent(String text) {
        return containsAny(text, "сабраж", "sabrage", "открыть шампан", "открыть игрист", "ритуал", "игровой момент");
    }

    private boolean isShortSafePlayCall(String text) {
        return text.equals("сабраж")
                || text.equals("хочу сабраж")
                || text.equals("сабражный ритуал")
                || text.equals("ритуал");
    }

    private boolean isDangerousHowTo(String text) {
        return containsAny(text, "научи", "как сделать", "как открыть", "самому", "дома", "инструкция", "объясни технику");
    }

    private boolean isMerchChainPurchaseIntent(String text) {
        return containsAny(text, "цепь", "цепочку", "цепи")
                && containsAny(text, "купить", "заказать", "предзаказ", "подарок", "мерч", "стоимость", "цена");
    }

    private boolean containsAny(String text, String... variants) {
        for (String variant : variants) {
            if (text.contains(variant)) {
                return true;
            }
        }
        return false;
    }

    private String displayName(IncomingMessage incoming) {
        String firstName = normalizeDisplay(incoming.firstName());
        String lastName = normalizeDisplay(incoming.lastName());
        String username = normalizeDisplay(incoming.username());
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (!username.isBlank()) {
            return "@" + username;
        }
        return "unknown";
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplay(String text) {
        return text == null ? "" : text.trim();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blankAsEmptyLabel(String value) {
        String normalized = normalizeDisplay(value);
        return normalized.isBlank() ? "(empty)" : normalized;
    }

    private String html(String value) {
        return text(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
