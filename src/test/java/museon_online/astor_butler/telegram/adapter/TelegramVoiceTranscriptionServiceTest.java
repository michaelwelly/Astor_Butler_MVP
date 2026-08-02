package museon_online.astor_butler.telegram.adapter;

import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.speech.SpeechToTextService;
import museon_online.astor_butler.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TelegramVoiceTranscriptionServiceTest {

    @Test
    void rejectsOversizedVoiceBeforeDownloadOrStt() {
        SpeechToTextService speechToTextService = mock(SpeechToTextService.class);
        ObjectStorageService objectStorageService = mock(ObjectStorageService.class);
        AbsSender sender = mock(AbsSender.class);
        TelegramVoiceTranscriptionService service = new TelegramVoiceTranscriptionService(
                speechToTextService,
                objectStorageService
        );
        ReflectionTestUtils.setField(service, "downloadEnabled", true);
        ReflectionTestUtils.setField(service, "maxDurationSeconds", 60L);
        ReflectionTestUtils.setField(service, "maxFileSizeBytes", 1024L);

        IncomingMessage enriched = service.enrich(telegramVoice(Map.of(
                "mediaKind", "VOICE",
                "telegramFileId", "file-id",
                "durationSeconds", 61,
                "fileSizeBytes", 100
        )), sender);

        assertThat(enriched.payload()).containsEntry("transcriptionStatus", "FAILED");
        assertThat(enriched.payload()).containsEntry("transcriptionReason", "VOICE_DURATION_LIMIT_EXCEEDED");
        verifyNoInteractions(sender, speechToTextService, objectStorageService);
    }

    private IncomingMessage telegramVoice(Map<String, Object> payload) {
        return IncomingMessage.telegram(
                1773317437L,
                1773317437L,
                351,
                284069875,
                "",
                null,
                "Наталья",
                "Поединенко",
                "Poedinenko",
                "ru",
                false,
                "284069875",
                payload
        );
    }
}
