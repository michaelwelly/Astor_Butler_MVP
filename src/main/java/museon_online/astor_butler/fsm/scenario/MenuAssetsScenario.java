package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MenuAssetsScenario implements FsmScenario {

    private final FSMStorage fsmStorage;
    private final AerisMediaCatalog mediaCatalog;

    public String id() {
        return "MENU_ASSETS";
    }

    public int priority() {
        return 40;
    }

    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return state == BotState.MENU_ASSETS_CLARIFY
                || state == BotState.MENU_ASSETS_DELIVERED
                || ((state == BotState.READY_FOR_DIALOG || state == BotState.AI_FALLBACK) && isMenuIntent(normalized));
    }

    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        List<MediaAsset> documents = classify(normalize(text));
        if (documents.isEmpty()) {
            fsmStorage.setState(incoming.chatId(), BotState.MENU_ASSETS_CLARIFY);
            return OutgoingMessage.of(
                    incoming,
                    "Уточните, пожалуйста: отправить кухню, бар, коктейли, вино или все меню?",
                    BotState.MENU_ASSETS_CLARIFY.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("MENU_ASSETS_CLARIFY", "ASK_MENU_CATEGORY")
            );
        }

        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        String textResponse = responseFor(documents);
        return OutgoingMessage.of(
                incoming,
                textResponse,
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("MENU_ASSETS", "MENU_ASSETS_DELIVERED", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "documents", documents.stream().map(this::metadata).toList(),
                "ragSource", mediaCatalog.menuRagSource(),
                "scenario", "MenuAssetsScenario"
        ));
    }

    private List<MediaAsset> classify(String text) {
        if (text.isBlank()) {
            return List.of();
        }
        boolean wantsAll = containsAny(text, "все меню", "полное меню", "скинь меню", "покажи меню", "меню", "что у вас есть");
        boolean wantsKitchen = containsAny(text, "кух", "еда", "поесть", "блюд", "основное");
        boolean wantsBar = containsAny(text, "бар", "напит", "крепк", "безалк", "барная");
        boolean wantsElements = containsAny(text, "коктей", "elements", "элемент", "авторск");
        boolean wantsWine = containsAny(text, "вино", "вину", "вин", "шампан", "игрист");

        if (wantsAll && !wantsWine && !wantsElements && !wantsBar && !wantsKitchen) {
            return mediaCatalog.allMenus();
        }

        List<MediaAsset> documents = new ArrayList<>();
        if (wantsKitchen) {
            documents.add(mediaCatalog.kitchenMenu());
        }
        if (wantsBar) {
            documents.add(mediaCatalog.barMenu());
        }
        if (wantsElements) {
            documents.add(mediaCatalog.elementsMenu());
        }
        if (wantsWine) {
            documents.add(mediaCatalog.wineMenu());
        }
        if (documents.isEmpty() && wantsAll) {
            return mediaCatalog.allMenus();
        }
        return List.copyOf(documents);
    }

    private String responseFor(List<MediaAsset> documents) {
        if (documents.size() == 4) {
            return """
                    Конечно. Отправляю актуальные карты AERIS: кухню, бар, коктейли Elements и винную карту.

                    Если захотите, я помогу выбрать стол или позову менеджера.
                    """;
        }
        String names = String.join(", ", documents.stream().map(MediaAsset::title).toList());
        return """
                Да, отправляю: %s.

                Если нужно, следом могу прислать остальные карты или помочь с бронью стола.
                """.formatted(names);
    }

    private boolean isMenuIntent(String text) {
        return containsAny(
                text,
                "меню",
                "карта бара",
                "барная карта",
                "бар",
                "напит",
                "коктей",
                "elements",
                "элемент",
                "вино",
                "вин",
                "кух",
                "еда",
                "поесть",
                "что у вас есть"
        ) && !text.equals("/menu") && !text.contains("главное меню");
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
        return canonical == BotState.MENU_ASSETS_CLARIFY || canonical == BotState.MENU_ASSETS_DELIVERED;
    }

    public boolean canRunInParallel() {
        return true;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, String> metadata(MediaAsset asset) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("assetCode", asset.assetCode());
        metadata.put("objectKey", asset.objectKey());
        metadata.put("filename", asset.filename());
        metadata.put("caption", asset.title());
        metadata.put("contentType", asset.contentType());
        return metadata;
    }
}
