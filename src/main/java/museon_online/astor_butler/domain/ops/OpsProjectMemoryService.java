package museon_online.astor_butler.domain.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.domain.semantic.EmbeddingProvider;
import museon_online.astor_butler.domain.semantic.SemanticChunkSeed;
import museon_online.astor_butler.domain.semantic.SemanticMemoryRepository;
import museon_online.astor_butler.domain.semantic.SemanticRetrievalService;
import museon_online.astor_butler.domain.semantic.SemanticSearchResult;
import museon_online.astor_butler.service.message.IncomingMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpsProjectMemoryService {

    public static final String SOURCE_CODE = "SMART_SOLUTION_GROUP_MEMORY";
    private static final String VENUE_CODE = "AERIS";
    private static final List<String> SOURCE_CODES = List.of(SOURCE_CODE);

    private final SemanticMemoryRepository semanticMemoryRepository;
    private final SemanticRetrievalService semanticRetrievalService;
    private final ObjectProvider<EmbeddingProvider> embeddingProvider;

    public void rememberHumanAnswer(OpsGroupQuestion question, IncomingMessage answer) {
        if (question == null || answer == null || answer.text() == null || answer.text().isBlank()) {
            return;
        }

        String content = """
                Вопрос из командного чата:
                %s

                Ответ Майкла/команды:
                %s
                """.formatted(question.questionText(), answer.text().trim()).strip();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("domain", "SMART_SOLUTION_OPS");
        metadata.put("ragScope", "group_qa");
        metadata.put("chatId", question.chatId());
        metadata.put("questionMessageId", question.messageId());
        metadata.put("answerMessageId", answer.telegramMessageId());
        metadata.put("answeredBy", displayName(answer));

        UUID chunkId = semanticMemoryRepository.upsertChunk(new SemanticChunkSeed(
                SOURCE_CODE,
                chunkKey(question),
                0,
                "ru",
                title(question),
                content,
                metadata
        ));
        upsertEmbedding(chunkId, content);
    }

    public void rememberGroupMessage(IncomingMessage incoming, OpsGroupMessageClassification classification) {
        if (incoming == null || incoming.text() == null || incoming.text().isBlank() || classification == null) {
            return;
        }

        String content = """
                FSM Scenario: %s
                Intent: %s
                Project: %s
                Result/Status: %s
                Author: %s

                Сообщение команды:
                %s
                """.formatted(
                classification.scenario(),
                classification.intent(),
                blank(classification.projectCode()),
                blank(classification.resultStatus()),
                displayName(incoming),
                incoming.text().trim()
        ).strip();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("domain", "SMART_SOLUTION_OPS");
        metadata.put("ragScope", "group_stream");
        metadata.put("fsmScenario", classification.scenario());
        metadata.put("intent", classification.intent());
        metadata.put("projectCode", classification.projectCode());
        metadata.put("resultStatus", classification.resultStatus());
        metadata.put("chatId", incoming.chatId());
        metadata.put("messageId", incoming.telegramMessageId());
        metadata.put("author", displayName(incoming));

        UUID chunkId = semanticMemoryRepository.upsertChunk(new SemanticChunkSeed(
                SOURCE_CODE,
                groupMessageChunkKey(incoming),
                0,
                "ru",
                groupMessageTitle(incoming, classification),
                content,
                metadata
        ));
        upsertEmbedding(chunkId, content);
    }

    public List<SemanticSearchResult> search(String query, int limit) {
        List<SemanticSearchResult> semantic = semanticRetrievalService.search(VENUE_CODE, query, SOURCE_CODES, limit);
        if (!semantic.isEmpty()) {
            return semantic;
        }
        return semanticMemoryRepository.searchText(VENUE_CODE, SOURCE_CODES, query, limit);
    }

    private void upsertEmbedding(UUID chunkId, String content) {
        try {
            EmbeddingProvider provider = embeddingProvider.getIfAvailable();
            if (provider == null) {
                return;
            }
            List<Double> embedding = provider.embed(content);
            if (!embedding.isEmpty()) {
                semanticMemoryRepository.upsertEmbedding(chunkId, provider.model(), embedding);
            }
        } catch (RuntimeException ex) {
            log.warn("Smart Solution memory embedding skipped: chunkId={} reason={}", chunkId, ex.toString());
        }
    }

    private String chunkKey(OpsGroupQuestion question) {
        return "telegram-group-qa-%s-%s".formatted(question.chatId(), question.messageId());
    }

    private String groupMessageChunkKey(IncomingMessage incoming) {
        if (incoming.telegramMessageId() != null) {
            return "telegram-group-message-%s-%s".formatted(incoming.chatId(), incoming.telegramMessageId());
        }
        return "telegram-group-message-%s-%s".formatted(incoming.chatId(), incoming.correlationId());
    }

    private String title(OpsGroupQuestion question) {
        String text = question.questionText() == null ? "Вопрос команды" : question.questionText().replaceAll("\\s+", " ").trim();
        return text.length() <= 120 ? text : text.substring(0, 117) + "...";
    }

    private String groupMessageTitle(IncomingMessage incoming, OpsGroupMessageClassification classification) {
        String project = classification.projectCode() == null ? "OPS" : classification.projectCode();
        String text = incoming.text() == null ? "" : incoming.text().replaceAll("\\s+", " ").trim();
        String shortText = text.length() <= 90 ? text : text.substring(0, 87) + "...";
        return "%s / %s / %s".formatted(project, classification.intent(), shortText);
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "not set" : value;
    }

    private String displayName(IncomingMessage incoming) {
        String username = incoming.username() == null ? "" : incoming.username().trim();
        if (!username.isBlank()) {
            return "@" + username;
        }
        String firstName = incoming.firstName() == null ? "" : incoming.firstName().trim();
        String lastName = incoming.lastName() == null ? "" : incoming.lastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "unknown" : fullName;
    }
}
