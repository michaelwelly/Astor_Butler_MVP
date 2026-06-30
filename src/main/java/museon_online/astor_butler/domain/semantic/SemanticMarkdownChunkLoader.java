package museon_online.astor_butler.domain.semantic;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SemanticMarkdownChunkLoader {

    public List<SemanticChunkSeed> load(Resource resource) {
        List<Section> sections = readSections(resource);
        List<SemanticChunkSeed> chunks = new ArrayList<>();
        int index = 0;
        for (Section section : sections) {
            if (section.content().isBlank()) {
                continue;
            }
            chunks.add(new SemanticChunkSeed(
                    section.sourceCode(),
                    chunkKey(resource, section.title(), index),
                    index,
                    "ru",
                    section.title(),
                    section.content(),
                    Map.of(
                            "seedResource", safeFilename(resource),
                            "sectionTitle", section.title()
                    )
            ));
            index++;
        }
        return chunks;
    }

    private List<Section> readSections(Resource resource) {
        List<Section> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String title = safeFilename(resource);
            String sourceCode = "";
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("## ")) {
                    addSection(result, title, sourceCode, content);
                    title = line.substring(3).trim();
                    sourceCode = "";
                    content = new StringBuilder();
                    continue;
                }
                if (line.startsWith("Source: ")) {
                    sourceCode = line.substring("Source: ".length()).replace(".", "").trim();
                    continue;
                }
                if (!line.startsWith("# ")) {
                    content.append(line).append('\n');
                }
            }
            addSection(result, title, sourceCode, content);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read semantic seed " + resource, e);
        }
        return result;
    }

    private void addSection(List<Section> result, String title, String sourceCode, StringBuilder content) {
        String compact = content.toString().replaceAll("\\n{3,}", "\n\n").trim();
        if (compact.isBlank()) {
            return;
        }
        result.add(new Section(title, sourceCode.isBlank() ? inferSource(title) : sourceCode, compact));
    }

    private String inferSource(String title) {
        String normalized = title == null ? "" : title.toLowerCase(Locale.ROOT);
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("кух", "AERIS_MENU_KITCHEN_SOURCE");
        mapping.put("бар", "AERIS_MENU_BAR_SOURCE");
        mapping.put("коктей", "AERIS_MENU_ELEMENTS_SOURCE");
        mapping.put("elements", "AERIS_MENU_ELEMENTS_SOURCE");
        mapping.put("вин", "AERIS_MENU_WINE_SOURCE");
        mapping.put("афиш", "ASTOR_FSM_SCENARIOS_SOURCE");
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "AERIS_GUEST_GUIDE_SOURCE";
    }

    private String chunkKey(Resource resource, String title, int index) {
        String raw = safeFilename(resource) + "-" + index + "-" + title;
        return raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9а-яё]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String safeFilename(Resource resource) {
        String filename = resource.getFilename();
        return filename == null || filename.isBlank() ? "semantic-seed" : filename;
    }

    private record Section(String title, String sourceCode, String content) {
    }
}
