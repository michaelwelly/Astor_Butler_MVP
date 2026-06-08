package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
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
public class QuietGuideScenario {

    private final FSMStorage fsmStorage;

    @Value("${telegram.quiet-guide.interior-video-object-key:content/aeris/interior/INTERIOR.mp4}")
    private String interiorVideoObjectKey;

    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return state == BotState.QUIET_GUIDE_CLARIFY
                || state == BotState.QUIET_GUIDE_DELIVERED
                || ((state == BotState.READY_FOR_DIALOG || state == BotState.AI_FALLBACK) && isQuietGuideIntent(normalized));
    }

    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        String normalized = normalize(text);
        if (isInteriorIntent(normalized)) {
            return videoTour(incoming);
        }
        if (isConceptIntent(normalized)) {
            return concept(incoming);
        }
        if (isPosterIntent(normalized)) {
            return ready(
                    incoming,
                    """
                    Афишу держу как тихую справку: без рассылки и лишнего шума.

                    Сейчас могу передать запрос менеджеру или помочь с бронью стола на нужный день.
                    """,
                    List.of("QUIET_GUIDE", "POSTER_LOOKUP", "RETURN_MAIN_MENU"),
                    Map.of("scenario", "QuietGuideScenario", "contentKind", "POSTER")
            );
        }
        fsmStorage.setState(incoming.chatId(), BotState.QUIET_GUIDE_CLARIFY);
        return OutgoingMessage.of(
                incoming,
                "Уточните, пожалуйста: показать ресторан внутри, рассказать концепцию, прислать меню, афишу или позвать менеджера?",
                BotState.QUIET_GUIDE_CLARIFY.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("QUIET_GUIDE_CLARIFY", "ASK_GUIDE_TOPIC")
        );
    }

    private OutgoingMessage videoTour(IncomingMessage incoming) {
        return ready(
                incoming,
                """
                Конечно. Отправляю короткий видео-тур по AERIS, чтобы можно было почувствовать зал до визита.

                После просмотра могу помочь выбрать стол или рассказать о концепции кухни.
                """,
                List.of("QUIET_GUIDE", "INTERIOR_VIDEO", "QUIET_GUIDE_DELIVERED", "RETURN_MAIN_MENU"),
                Map.of(
                        "scenario", "QuietGuideScenario",
                        "videoObjectKey", interiorVideoObjectKey,
                        "videoFilename", "INTERIOR.mp4",
                        "videoCaption", "AERIS interior tour",
                        "videoSendMode", "DOCUMENT"
                )
        );
    }

    private OutgoingMessage concept(IncomingMessage incoming) {
        return ready(
                incoming,
                """
                <b>Гастрономическая экспедиция Георгия Матвеева в AERIS</b>

                AERIS открывает новую главу: кухню 21 страны Средиземноморья в прочтении Георгия Матвеева — шеф-повара с золотым почерком, победителя пятого сезона "Адской кухни" и обладателя Гран-при международных кулинарных чемпионатов.

                Его концепция — масштабное исследование Средиземноморского бассейна: от Франции и Италии до пряных традиций Ливана. В меню около 80 авторских позиций, где исторические вкусы соединены с эстетикой яркого гастробара.

                Философия шефа проста: торжество продукта и чистота вкуса. В фокусе премиальное мясо, свежая рыба, зелень и авторские неклассические соусы.

                Могу следом прислать меню кухни или помочь выбрать стол.
                """,
                true,
                List.of("QUIET_GUIDE", "CONCEPT_COPY", "QUIET_GUIDE_DELIVERED", "RETURN_MAIN_MENU"),
                Map.of("scenario", "QuietGuideScenario", "contentKind", "CONCEPT")
        );
    }

    private OutgoingMessage ready(IncomingMessage incoming, String text, List<String> actions, Map<String, Object> metadata) {
        return ready(incoming, text, false, actions, metadata);
    }

    private OutgoingMessage ready(IncomingMessage incoming, String text, boolean html, List<String> actions, Map<String, Object> metadata) {
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                text,
                BotState.READY_FOR_DIALOG.name(),
                html,
                false,
                true,
                false,
                AdminAlert.none(),
                actions
        ).withMetadata(metadata);
    }

    private boolean isQuietGuideIntent(String text) {
        return isInteriorIntent(text)
                || isConceptIntent(text)
                || isPosterIntent(text)
                || containsAny(text, "справ", "что сегодня", "что у вас", "как у вас", "расскажи про aeris");
    }

    private boolean isInteriorIntent(String text) {
        return containsAny(text, "интерьер", "внутри", "зал", "видео", "тур", "показать ресторан", "покажи ресторан", "посмотреть ресторан");
    }

    private boolean isConceptIntent(String text) {
        return containsAny(text, "концепц", "шеф", "георг", "матве", "адская кухня", "что за место", "расскажи про aeris", "философ");
    }

    private boolean isPosterIntent(String text) {
        return containsAny(text, "афиша", "событ", "расписание", "что сегодня", "что будет");
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }
}
