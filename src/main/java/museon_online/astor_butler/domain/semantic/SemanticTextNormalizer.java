package museon_online.astor_butler.domain.semantic;

import java.util.Locale;

public final class SemanticTextNormalizer {

    private SemanticTextNormalizer() {
    }

    public static String normalize(String text) {
        return text == null
                ? ""
                : text.trim()
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("\\s+", " ");
    }
}
