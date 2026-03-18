package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record CaseAnalysisResponse(
        String status,
        List<TimelineEntry> timeline,
        List<String> faits,
        List<String> pointsJuridiques,
        List<String> risques,
        List<String> questionsOuvertes,
        String modelUsed,
        Instant updatedAt
) {

    public record TimelineEntry(String date, String evenement) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static CaseAnalysisResponse from(CaseAnalysis analysis) {
        List<TimelineEntry> timeline = List.of();
        List<String> faits = List.of();
        List<String> pointsJuridiques = List.of();
        List<String> risques = List.of();
        List<String> questionsOuvertes = List.of();

        String raw = analysis.getAnalysisResult();
        if (raw != null && !raw.isBlank()) {
            try {
                JsonNode root = MAPPER.readTree(raw);
                timeline = extractTimeline(root);
                faits = extractStringList(root, "faits");
                pointsJuridiques = extractStringList(root, "points_juridiques");
                risques = extractStringList(root, "risques");
                questionsOuvertes = extractStringList(root, "questions_ouvertes");
            } catch (Exception ignored) {
                // JSON malformé — on retourne les listes vides
            }
        }

        return new CaseAnalysisResponse(
                analysis.getAnalysisStatus().name(),
                timeline,
                faits,
                pointsJuridiques,
                risques,
                questionsOuvertes,
                analysis.getModelUsed(),
                analysis.getUpdatedAt()
        );
    }

    private static List<String> extractStringList(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) result.add(item.asText());
        }
        return List.copyOf(result);
    }

    private static List<TimelineEntry> extractTimeline(JsonNode root) {
        JsonNode node = root.get("timeline");
        if (node == null || !node.isArray()) return List.of();
        List<TimelineEntry> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isObject()) {
                String date = item.has("date") ? item.get("date").asText() : "";
                String evenement = item.has("evenement") ? item.get("evenement").asText() : "";
                result.add(new TimelineEntry(date, evenement));
            }
        }
        return List.copyOf(result);
    }
}
