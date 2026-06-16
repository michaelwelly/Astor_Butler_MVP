package museon_online.astor_butler.domain.booking;

import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.telegram.adapter.TelegramMediaSender;
import museon_online.astor_butler.telegram.utils.TelegramBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TableReservationPendingIntentServiceTest {

    private final FSMStorage fsmStorage = mock(FSMStorage.class);
    private final AerisMediaCatalog mediaCatalog = mock(AerisMediaCatalog.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<TelegramBot> telegramBotProvider = mock(ObjectProvider.class);
    private final TelegramMediaSender telegramMediaSender = mock(TelegramMediaSender.class);
    private final TelegramBot telegramBot = mock(TelegramBot.class);

    private TableReservationPendingIntentService service;

    @BeforeEach
    void setUp() {
        service = new TableReservationPendingIntentService(
                fsmStorage,
                mediaCatalog,
                telegramBotProvider,
                telegramMediaSender
        );
        ReflectionTestUtils.setField(service, "telegramEnabled", true);
        ReflectionTestUtils.setField(service, "notificationsEnabled", true);
    }

    @Test
    void deliversPendingWineMenuAfterTableConfirmation() {
        Long chatId = 1773317437L;
        when(fsmStorage.getPendingIntents(chatId))
                .thenReturn(List.of("MENU_ASSETS::покажи винную карту"));
        when(telegramBotProvider.getIfAvailable()).thenReturn(telegramBot);
        when(mediaCatalog.wineMenu()).thenReturn(asset(
                "AERIS_MENU_WINE",
                "Винная карта",
                "content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf",
                "WINE MENU 2026 FINAL.pdf"
        ));

        service.deliverAfterConfirmation(order(chatId));

        verify(telegramMediaSender).sendDocumentIfPresent(
                eq(chatId),
                argThat(TableReservationPendingIntentServiceTest::hasWineDocument),
                eq(telegramBot)
        );
        verify(fsmStorage).clearPendingIntents(chatId);
    }

    @Test
    void keepsPendingIntentsWhenTelegramBotIsDisabled() {
        Long chatId = 1773317437L;
        when(fsmStorage.getPendingIntents(chatId))
                .thenReturn(List.of("MENU_ASSETS::покажи меню"));
        ReflectionTestUtils.setField(service, "telegramEnabled", false);

        service.deliverAfterConfirmation(order(chatId));

        verify(telegramMediaSender, never()).sendDocumentIfPresent(eq(chatId), org.mockito.ArgumentMatchers.any(), eq(telegramBot));
        verify(fsmStorage, never()).clearPendingIntents(chatId);
    }

    @SuppressWarnings("unchecked")
    private static boolean hasWineDocument(Map<String, Object> metadata) {
        Object rawDocuments = metadata.get("documents");
        if (!(rawDocuments instanceof List<?> documents) || documents.size() != 1) {
            return false;
        }
        Object rawDocument = documents.get(0);
        if (!(rawDocument instanceof Map<?, ?> document)) {
            return false;
        }
        return "content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf".equals(document.get("objectKey"))
                && "WINE MENU 2026 FINAL.pdf".equals(document.get("filename"))
                && "Винная карта".equals(document.get("caption"));
    }

    private TableReservationOrder order(Long chatId) {
        return new TableReservationOrder(
                12L,
                chatId,
                chatId,
                null,
                5L,
                "5",
                "Стол 5",
                null,
                null,
                TableReservationStatus.CONFIRMED,
                "TELEGRAM",
                Instant.parse("2026-06-06T17:00:00Z"),
                Instant.parse("2026-06-06T19:00:00Z"),
                2,
                "Наталья",
                "+79990000000",
                "Хочу винную карту после брони",
                876857557L,
                null,
                null,
                null,
                Instant.parse("2026-06-05T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z")
        );
    }

    private MediaAsset asset(String assetCode, String title, String objectKey, String filename) {
        return new MediaAsset(
                assetCode,
                "AERIS",
                "QUIET_GUIDE",
                "PDF_MENU",
                title,
                "astor-media",
                objectKey,
                filename,
                "application/pdf",
                true
        );
    }
}
