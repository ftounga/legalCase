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
        if (json == null || json.isBlank()) return json;
        String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(json);
        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(stripped);
            truncateArray(root, "faits", limits.getFaits());
            truncateArray(root, "points_juridiques", limits.getPointsJuridiques());
            truncateArray(root, "risques", limits.getRisques());
            truncateArray(root, "questions_ouvertes", limits.getQuestionsOuvertes());
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return json;
        }
    }

    static String truncateCaseAnalysis(String json, AnalysisLimitsProperties.LevelLimits limits) {
        if (json == null || json.isBlank()) return json;
        String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(json);
        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(stripped);
            truncateArray(root, "faits", limits.getFaits());
            truncateArray(root, "points_juridiques", limits.getPointsJuridiques());
            truncateArray(root, "risques", limits.getRisques());
            truncateArray(root, "questions_ouvertes", limits.getQuestionsOuvertes());
            truncateArray(root, "timeline", limits.getTimeline());
            truncateArray(root, "pieces_manquantes", limits.getPiecesManquantes());
            truncateArray(root, "points_procedure", limits.getPointsProcedure());
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
