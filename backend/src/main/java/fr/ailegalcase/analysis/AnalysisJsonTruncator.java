package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tronque les tableaux d'un JSON d'analyse Claude à des limites définies.
 * Garantit que les limites SF-28 sont respectées quelle que soit la réponse du modèle.
 */
class AnalysisJsonTruncator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AnalysisJsonTruncator() {}

    /**
     * Tronque les tableaux d'une analyse de document.
     * Limites : faits=5, points_juridiques=3, risques=3, questions_ouvertes=3
     */
    static String truncateDocumentAnalysis(String json) {
        return truncate(json, 5, 3, 3, 3, Integer.MAX_VALUE);
    }

    /**
     * Tronque les tableaux d'une analyse de dossier.
     * Limites : timeline=5, faits=7, points_juridiques=5, risques=5, questions_ouvertes=5
     */
    static String truncateCaseAnalysis(String json) {
        return truncate(json, 7, 5, 5, 5, 5);
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
