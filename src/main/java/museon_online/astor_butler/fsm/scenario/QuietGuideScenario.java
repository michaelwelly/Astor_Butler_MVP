package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.content.VenueContentPost;
import museon_online.astor_butler.domain.content.VenueContentQueryService;
import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
import museon_online.astor_butler.domain.semantic.SemanticRetrievalService;
import museon_online.astor_butler.domain.semantic.SemanticSearchResult;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.reply.ScenarioReply;
import museon_online.astor_butler.fsm.reply.ScenarioReplyComposer;
import museon_online.astor_butler.fsm.reply.ScenarioReplyDraft;
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
public class QuietGuideScenario implements FsmScenario {

    private final FSMStorage fsmStorage;
    private final AerisMediaCatalog mediaCatalog;
    private final VenueContentQueryService venueContentQueryService;
    private final SemanticRetrievalService semanticRetrievalService;
    private final ScenarioReplyComposer replyComposer;

    @Value("${telegram.quiet-guide.interior-video-asset-code:AERIS_INTERIOR_TOUR}")
    private String interiorVideoAssetCode;

    public String id() {
        return "QUIET_GUIDE";
    }

    public int priority() {
        return 50;
    }

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
            return concept(incoming, text);
        }
        if (isPosterIntent(normalized)) {
            return poster(incoming, text);
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
        MediaAsset video = mediaCatalog.interiorTour();
        return ready(
                incoming,
                """
                Конечно. Отправляю короткий видео-тур по AERIS, чтобы можно было почувствовать зал до визита.

                После просмотра могу помочь выбрать стол или рассказать о концепции кухни.
                """,
                List.of("QUIET_GUIDE", "INTERIOR_VIDEO", "QUIET_GUIDE_DELIVERED", "RETURN_MAIN_MENU"),
                Map.of(
                        "scenario", "QuietGuideScenario",
                        "videoAssetCode", interiorVideoAssetCode,
                        "videoObjectKey", video.objectKey(),
                        "videoFilename", video.filename(),
                        "videoCaption", video.title(),
                        "videoSendMode", "DOCUMENT"
                )
        );
    }

    private OutgoingMessage concept(IncomingMessage incoming, String prompt) {
        List<SemanticSearchResult> ragContext = semanticRetrievalService.search(
                "AERIS",
                prompt,
                List.of("AERIS_GUEST_GUIDE_SOURCE"),
                2
        );
        String fallbackText = """
                <b>Гастрономическая экспедиция Георгия Матвеева в AERIS</b>

                AERIS открывает новую главу: кухню 21 страны Средиземноморья в прочтении Георгия Матвеева — шеф-повара с золотым почерком, победителя пятого сезона "Адской кухни" и обладателя Гран-при международных кулинарных чемпионатов.

                Его концепция — масштабное исследование Средиземноморского бассейна: от Франции и Италии до пряных традиций Ливана. В меню около 80 авторских позиций, где исторические вкусы соединены с эстетикой яркого гастробара.

                Философия шефа проста: торжество продукта и чистота вкуса. В фокусе премиальное мясо, свежая рыба, зелень и авторские неклассические соусы.

                Могу следом прислать меню кухни или помочь выбрать стол.
                """;
        ScenarioReply reply = replyComposer.compose(ScenarioReplyDraft.of(
                incoming,
                "AERIS",
                id(),
                BotState.READY_FOR_DIALOG.name(),
                "QUIET_GUIDE_CONCEPT_COPY",
                prompt,
                fallbackText,
                ragContext
        ));
        return ready(
                incoming,
                reply.text(),
                !reply.generated(),
                List.of("QUIET_GUIDE", "CONCEPT_COPY", "QUIET_GUIDE_DELIVERED", "RETURN_MAIN_MENU"),
                Map.of(
                        "scenario", "QuietGuideScenario",
                        "contentKind", "CONCEPT",
                        "ragContext", ragContext.stream().map(this::ragMetadata).toList(),
                        "replyGenerated", reply.generated(),
                        "replyProvider", reply.provider(),
                        "replyModel", reply.model()
                )
        );
    }

    private OutgoingMessage poster(IncomingMessage incoming, String prompt) {
        List<VenueContentPost> posts = venueContentQueryService.activeQuietGuidePosts("AERIS", prompt);
        if (posts.isEmpty()) {
            return ready(
                    incoming,
                    """
                    Афишу держу как тихую справку: без рассылки и лишнего шума.

                    На сейчас могу подсказать постоянный повод заглянуть: с воскресенья по четверг в AERIS действует винный безлимит за 1700 ₽.

                    По пятнице и субботе лучше уточнить афишу недели: как только свежий пост появится в канале AERIS, я подтяну событие сюда. Могу также показать меню, видео-тур или позвать менеджера.
                    """,
                    List.of("QUIET_GUIDE", "POSTER_LOOKUP_EMPTY", "RETURN_MAIN_MENU"),
                    Map.of("scenario", "QuietGuideScenario", "contentKind", "POSTER")
            );
        }

        StringBuilder text = new StringBuilder("""
                <b>Актуальное из AERIS</b>

                """);
        for (int i = 0; i < posts.size(); i++) {
            VenueContentPost post = posts.get(i);
            text.append(i + 1)
                    .append(". <b>")
                    .append(escapeHtml(post.title()))
                    .append("</b>\n")
                    .append(escapeHtml(shortBody(post.body())));
            if (post.sourceUrl() != null && !post.sourceUrl().isBlank()) {
                text.append("\n<a href=\"")
                        .append(escapeHtml(post.sourceUrl()))
                        .append("\">Открыть пост</a>");
            }
            if (i + 1 < posts.size()) {
                text.append("\n\n");
            }
        }
        text.append("\n\nМогу помочь забронировать стол под выбранный день.");

        return ready(
                incoming,
                text.toString(),
                true,
                List.of("QUIET_GUIDE", "POSTER_LOOKUP", "CONTENT_POSTS_FOUND", "RETURN_MAIN_MENU"),
                Map.of(
                        "scenario", "QuietGuideScenario",
                        "contentKind", "POSTER",
                        "contentPostIds", posts.stream().map(post -> post.id().toString()).toList(),
                        "sourceUrls", posts.stream().map(VenueContentPost::sourceUrl).toList()
                )
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
                || containsAny(text, "справ", "что сегодня", "что у вас", "как у вас", "расскажи про aeris", "расскажи про ресторан", "расскажи о ресторане", "про ресторан", "о ресторане", "про заведение", "о заведении");
    }

    private boolean isInteriorIntent(String text) {
        return containsAny(text, "интерьер", "внутри", "зал", "видео", "тур", "показать ресторан", "покажи ресторан", "посмотреть ресторан");
    }

    private boolean isConceptIntent(String text) {
        return containsAny(text, "концепц", "шеф", "георг", "матве", "адская кухня", "что за место", "что за ресторан", "расскажи про aeris", "расскажи про ресторан", "расскажи о ресторане", "про ресторан", "о ресторане", "про заведение", "о заведении", "философ");
    }

    private boolean isPosterIntent(String text) {
        return containsAny(text, "афиш", "событ", "расписание", "что сегодня", "что будет");
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.QUIET_GUIDE_CLARIFY || canonical == BotState.QUIET_GUIDE_DELIVERED;
    }

    public boolean canRunInParallel() {
        return true;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String shortBody(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() > 260 ? compact.substring(0, 257) + "..." : compact;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private Map<String, Object> ragMetadata(SemanticSearchResult result) {
        return Map.of(
                "sourceCode", result.sourceCode(),
                "sourceType", result.sourceType(),
                "title", result.title(),
                "score", result.score(),
                "content", result.shortContent(360)
        );
    }
}
