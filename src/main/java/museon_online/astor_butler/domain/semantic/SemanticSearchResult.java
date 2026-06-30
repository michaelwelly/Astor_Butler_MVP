package museon_online.astor_butler.domain.semantic;

import java.util.UUID;

public record SemanticSearchResult(
        UUID chunkId,
        String sourceCode,
        String sourceType,
        String title,
        String content,
        double score
) {
    public String shortContent(int maxLength) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
