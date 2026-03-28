package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tronque les tableaux d'un JSON d'analyse Claude aux limites définies dans AnalysisLimitsProperties.
 */
class AnalysisJsonTruncator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AnalysisJsonTruncator() {}

    static String truncateDocumentAnalysis(String json, AnalysisLimitsProperties.LevelLimits limits) {
        return truncate(json,
                limits.getFaits(),
                limits.getPointsJuridiques(),
                limits.getRisques(),
                limits.getQuestionsOuvertes(),
                Integer.MAX_VALUE);
    }

    static String truncateCaseAnalysis(String json, AnalysisLimitsProperties.LevelLimits limits) {
        return truncate(json,
                limits.getFaits(),
                limits.getPointsJuridiques(),
                limits.getRisques(),
                limits.getQuestionsOuvertes(),
                limits.getTimeline());
    }

    private static String truncate(String json,
                                    int maxFaits,
                                    int maxPointsJuridiques,
                                    int maxRisques,
                                    int maxQuestionsOuvertes,
                                    int maxTimeline) {
        if (json == null || json.isBlank()) return json;
        String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(json);
        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(stripped);
            truncateArray(root, "faits", maxFaits);
            truncateArray(root, "points_juridiques", maxPointsJuridiques);
            truncateArray(root, "risques", maxRisques);
            truncateArray(root, "questions_ouvertes", maxQuestionsOuvertes);
            truncateArray(root, "timeline", maxTimeline);
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            // JSON invalide (ex: tronqué par max_tokens) — on retourne tel quel
            return json;
        }
    }

    private static void truncateArray(ObjectNode root, String field, int max) {
        if (max == Integer.MAX_VALUE) return;
        if (!root.has(field) || !root.get(field).isArray()) return;
        ArrayNode original = (ArrayNode) root.get(field);
        if (original.size() <= max) return;
        ArrayNode truncated = MAPPER.createArrayNode();
        for (int i = 0; i < max; i++) {
            truncated.add(original.get(i));
        }
        root.set(field, truncated);
    }
}
