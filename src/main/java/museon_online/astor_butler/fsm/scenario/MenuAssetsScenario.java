package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
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
public class MenuAssetsScenario {

    private static final MenuDocument KITCHEN = new MenuDocument(
            "Кухня / основное меню",
            "classpath:menu/aeris/MENU AERIS A4 2026 DIGITAL.pdf",
            "MENU AERIS A4 2026 DIGITAL.pdf"
    );
    private static final MenuDocument BAR = new MenuDocument(
            "Барная карта",
            "classpath:menu/aeris/BAR CARD.pdf",
            "BAR CARD.pdf"
    );
    private static final MenuDocument ELEMENTS = new MenuDocument(
            "Коктейли / Elements",
            "classpath:menu/aeris/ELEMENTS CARD.pdf",
            "ELEMENTS CARD.pdf"
    );
    private static final MenuDocument WINE = new MenuDocument(
            "Винная карта",
            "classpath:menu/aeris/WINE MENU 2026 FINAL.pdf",
            "WINE MENU 2026 FINAL.pdf"
    );

    private final FSMStorage fsmStorage;

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
        List<MenuDocument> documents = classify(normalize(text));
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
                "documents", documents.stream().map(MenuDocument::metadata).toList(),
                "ragSource", "classpath:menu/aeris/menu-rag-index.md",
                "scenario", "MenuAssetsScenario"
        ));
    }

    private List<MenuDocument> classify(String text) {
        if (text.isBlank()) {
            return List.of();
        }
        boolean wantsAll = containsAny(text, "все меню", "полное меню", "скинь меню", "покажи меню", "меню", "что у вас есть");
        boolean wantsKitchen = containsAny(text, "кух", "еда", "поесть", "блюд", "основное");
        boolean wantsBar = containsAny(text, "бар", "напит", "крепк", "безалк", "барная");
        boolean wantsElements = containsAny(text, "коктей", "elements", "элемент", "авторск");
        boolean wantsWine = containsAny(text, "вино", "вину", "вин", "шампан", "игрист");

        if (wantsAll && !wantsWine && !wantsElements && !wantsBar && !wantsKitchen) {
            return List.of(KITCHEN, BAR, ELEMENTS, WINE);
        }

        List<MenuDocument> documents = new ArrayList<>();
        if (wantsKitchen) {
            documents.add(KITCHEN);
        }
        if (wantsBar) {
            documents.add(BAR);
        }
        if (wantsElements) {
            documents.add(ELEMENTS);
        }
        if (wantsWine) {
            documents.add(WINE);
        }
        if (documents.isEmpty() && wantsAll) {
            return List.of(KITCHEN, BAR, ELEMENTS, WINE);
        }
        return List.copyOf(documents);
    }

    private String responseFor(List<MenuDocument> documents) {
        if (documents.size() == 4) {
            return """
                    Конечно. Отправляю актуальные карты AERIS: кухню, бар, коктейли Elements и винную карту.

                    Если захотите, я помогу выбрать стол или позову менеджера.
                    """;
        }
        String names = String.join(", ", documents.stream().map(MenuDocument::title).toList());
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

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private record MenuDocument(String title, String resource, String filename) {
        private Map<String, String> metadata() {
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("resource", resource);
            metadata.put("filename", filename);
            metadata.put("caption", title);
            return metadata;
        }
    }
}
