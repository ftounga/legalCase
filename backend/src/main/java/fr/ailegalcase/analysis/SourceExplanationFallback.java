package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fallback heuristique pour compléter les explanations Haiku manquantes
 * (SF-IA-03-20). Scanne la synthèse JSON (F-93 traçabilité) pour extraire
 * une source+extrait depuis un fait/point/risque dont le texte contient
 * un mot-clé associé au sourceKey.
 */
@Service
public class SourceExplanationFallback {

    private static final Logger log = LoggerFactory.getLogger(SourceExplanationFallback.class);
    private static final int MAX_SECONDARY_TEXT = 200;

    /** Map sourceKey → mots-clés indicatifs (normalisés lowercase sans accent). */
    private static final Map<String, List<String>> KEYWORDS = Map.ofEntries(
            Map.entry("convention_collective", List.of("convention", "ccn", "idcc", "collective")),
            Map.entry("salaire_brut_mensuel", List.of("salaire", "remuneration", "brut", "mensuel")),
            Map.entry("date_entree", List.of("entree", "embauche", "recrute", "embauche", "commence")),
            Map.entry("conges_contractuels", List.of("conges", "cp", "jours de conge")),
            Map.entry("prime_anciennete_contractuelle", List.of("prime", "anciennete")),
            Map.entry("type_rupture", List.of("licenciement", "rupture conventionnelle", "demission", "rupture")),
            Map.entry("date_licenciement", List.of("licenciement", "notification", "lettre")),
            Map.entry("duree_mariage", List.of("mariage", "epoux", "conjoint")),
            Map.entry("revenus_conjoints", List.of("revenus", "salaire conjoint")),
            Map.entry("nationalite_ue", List.of("nationalite", "ue", "ressortissant")),
            Map.entry("date_notification_decision_contestee", List.of("notification", "refus", "decision")),
            Map.entry("type_titre_sejour", List.of("titre", "sejour", "carte")),
            Map.entry("type_recours", List.of("recours", "contestation"))
    );

    private final ObjectMapper objectMapper;

    public SourceExplanationFallback(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Complète les explanations Haiku avec des fallbacks heuristiques pour les sourcekeys
     * classiques et les codes F-96 non déjà couverts.
     */
    public List<SourceExplanationData> buildFallbacks(String analysisJson,
                                                     List<Document> documents,
                                                     List<ProcedureCheck> f96Checks,
                                                     Set<String> existingSourceKeys) {
        List<SourceExplanationData> out = new ArrayList<>();
        List<Fait> faits = extractFaits(analysisJson);
        Map<String, UUID> docIdByFilename = buildDocIndex(documents);

        // Fallback sourceKeys classiques
        KEYWORDS.forEach((key, keywords) -> {
            if (existingSourceKeys.contains(key)) return;
            Fait match = findMatch(faits, keywords);
            if (match == null) return;
            UUID docId = match.source != null
                    ? docIdByFilename.get(match.source.toLowerCase(Locale.ROOT))
                    : null;
            String sourceType = match.source != null ? "DOCUMENT" : "ANALYSIS_DETECTION";
            String label = match.source != null ? match.source : "Synthèse du dossier";
            String secondary = match.extrait != null ? truncate(match.extrait, MAX_SECONDARY_TEXT) : null;
            out.add(new SourceExplanationData(key, sourceType, label, null, secondary,
                    docId, null, null, null, null));
        });

        // Fallback codes F-96 non couverts
        if (f96Checks != null) {
            for (ProcedureCheck c : f96Checks) {
                String code = c.getCritereCode();
                if (code == null || code.isBlank()) continue;
                if (existingSourceKeys.contains(code)) continue;
                String label = c.getDescription() != null ? c.getDescription() : code;
                String secondary = buildF96Secondary(c);
                out.add(new SourceExplanationData(code, "CHECKLIST_F96", label, null, secondary,
                        null, null, code, null, null));
            }
        }

        if (!out.isEmpty()) {
            log.debug("Built {} fallback source explanations", out.size());
        }
        return out;
    }

    private String buildF96Secondary(ProcedureCheck c) {
        String statut = c.getStatut() != null ? c.getStatut().name() : "INCONNU";
        String raison = c.getRaison();
        if (raison == null || raison.isBlank()) {
            return "Statut : " + statut;
        }
        return "Statut : " + statut + " — " + truncate(raison, MAX_SECONDARY_TEXT - 30);
    }

    private List<Fait> extractFaits(String analysisJson) {
        List<Fait> out = new ArrayList<>();
        if (analysisJson == null || analysisJson.isBlank()) return out;
        try {
            JsonNode root = objectMapper.readTree(analysisJson);
            for (String section : List.of("faits", "points_juridiques", "risques")) {
                JsonNode arr = root.get(section);
                if (arr == null || !arr.isArray()) continue;
                for (JsonNode item : arr) {
                    if (!item.isObject()) continue;
                    String texte = textOrNull(item, "texte");
                    String source = textOrNull(item, "source");
                    String extrait = textOrNull(item, "extrait");
                    if (texte != null) {
                        out.add(new Fait(texte, source, extrait));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse analysis JSON for fallback: {}", e.getMessage());
        }
        return out;
    }

    private Fait findMatch(List<Fait> faits, List<String> keywords) {
        for (Fait f : faits) {
            String normalized = normalize(f.texte);
            for (String kw : keywords) {
                if (normalized.contains(kw)) return f;
            }
        }
        return null;
    }

    private Map<String, UUID> buildDocIndex(List<Document> documents) {
        Map<String, UUID> index = new HashMap<>();
        if (documents == null) return index;
        for (Document doc : documents) {
            if (doc.getOriginalFilename() != null) {
                index.put(doc.getOriginalFilename().toLowerCase(Locale.ROOT), doc.getId());
            }
        }
        return index;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String stripped = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return stripped.toLowerCase(Locale.ROOT);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) return null;
        String s = child.asText();
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private record Fait(String texte, String source, String extrait) {}
}
