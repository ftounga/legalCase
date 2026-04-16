package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Génère, via Haiku en post-traitement synchrone de l'analyse dossier,
 * une phrase d'explication pédagogique pour chaque donnée factuelle extraite
 * (SF-IA-03-15a). Fail-open : toute erreur renvoie une liste vide, l'analyse
 * dossier reste DONE et le popover côté front tombe en fallback template.
 */
@Service
public class SourceExplanationGenerator {

    private static final Logger log = LoggerFactory.getLogger(SourceExplanationGenerator.class);
    private static final int MAX_TOKENS = 1024;
    private static final int MAX_SYNTHESIS_CHARS = 8000;

    private final AnthropicService anthropicService;
    private final ObjectMapper objectMapper;
    private final DocumentRepository documentRepository;

    public SourceExplanationGenerator(AnthropicService anthropicService, ObjectMapper objectMapper,
                                      DocumentRepository documentRepository) {
        this.anthropicService = anthropicService;
        this.objectMapper = objectMapper;
        this.documentRepository = documentRepository;
    }

    public List<SourceExplanationData> generate(CaseFile caseFile, String analysisJson) {
        if (analysisJson == null || analysisJson.isBlank()) return List.of();

        try {
            List<Document> documents = documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile);
            String userMessage = buildUserMessage(documents, analysisJson);
            String systemPrompt = buildSystemPrompt();
            AnthropicResult result = anthropicService.analyzeFast(systemPrompt, userMessage, MAX_TOKENS);
            return parse(result.content(), documents);
        } catch (Exception e) {
            log.warn("SourceExplanationGenerator failed for case {}: {} — fallback to empty list",
                    caseFile.getId(), e.getMessage());
            return List.of();
        }
    }

    private String buildSystemPrompt() {
        return """
                Tu es un assistant juridique qui reformule des données factuelles extraites d'un dossier
                en phrases pédagogiques pour un avocat.

                Pour chaque donnée factuelle importante identifiée dans la synthèse IA, produis UNE phrase
                d'explication (≤ 220 caractères) pédagogique et factuelle, qui permet à l'avocat de
                comprendre d'un coup d'œil d'où vient l'information et pourquoi elle fait foi.

                Tu produis un JSON strict, sans markdown, sans texte hors JSON, au format exact suivant :

                {
                  "explanations": [
                    {
                      "sourceKey": "convention_collective",
                      "sourceType": "DOCUMENT" | "QUESTION_AI" | "CHECKLIST_F96" | "CHAT" | "MISSING_PIECE" | "ANALYSIS_DETECTION",
                      "label": "nom du document | intitulé court de la source",
                      "sentence": "Phrase d'explication pédagogique ≤ 220 car.",
                      "anchorDocName": "nom exact du document si sourceType=DOCUMENT, sinon null"
                    }
                  ]
                }

                Règles impératives :
                - Une seule phrase par sourceKey.
                - sourceKey en snake_case (génériques) OU code critère F96 en UPPER_CASE (spécifiques outil).
                - Génériques (métier, transversaux aux outils) : convention_collective, date_entree,
                  salaire_brut_mensuel, conges_contractuels, prime_anciennete_contractuelle, type_rupture,
                  date_licenciement, type_titre_sejour, type_recours, duree_mariage, revenus_conjoints.
                - Codes critères F96 spécifiques à produire si l'info est identifiable dans la synthèse :
                  * F-DT-08 Validité licenciement (FR) : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION,
                    FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT.
                  * F-DT-08 Validité licenciement (BE) : BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION,
                    BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE.
                  * F-DT-09 Comparateur indemnités : DT09_TYPE_RUPTURE.
                  * F-DT-10 Rupture conventionnelle (FR) : RC_CONSENTEMENT, RC_DELAI_RETRACTATION,
                    RC_HOMOLOGATION, RC_ASSISTANCE, RC_INDEMNITE, RC_ENTRETIENS.
                  * F-FA-05 Partage immobilier : FA05_VALEUR_VENALE, FA05_CAPITAL_RESTANT.
                  * F-FA-06 Calendrier garde : FA06_MODE_GARDE.
                  * F-FA-07 Checklist divorce : codes étapes/pièces FR_* et BE_* (ex. FR_REDACTION_CONVENTION).
                - sourceType = DOCUMENT si la donnée est clairement extraite d'un document précis du dossier.
                - sourceType = ANALYSIS_DETECTION si la donnée est déduite de l'analyse globale sans document
                  unique identifiable.
                - anchorDocName = nom exact tel qu'il apparaît dans la liste des documents fournis.
                - N'invente pas de donnée : si une donnée n'est pas dans la synthèse, ne la cite pas.
                - Produis uniquement les sourcekeys pour lesquels la synthèse contient une information
                  concrète — les autres sont omis (pas d'entrée vide).
                - Pas de conseils juridiques, uniquement du factuel reformulé.
                - Rédige en français.
                """;
    }

    private String buildUserMessage(List<Document> documents, String analysisJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Documents du dossier\n");
        if (documents == null || documents.isEmpty()) {
            sb.append("(aucun)\n");
        } else {
            for (Document doc : documents) {
                if (doc.getOriginalFilename() != null) {
                    sb.append("- ").append(doc.getOriginalFilename()).append('\n');
                }
            }
        }
        sb.append("\n# Synthèse IA (JSON)\n");
        String truncated = analysisJson.length() > MAX_SYNTHESIS_CHARS
                ? analysisJson.substring(0, MAX_SYNTHESIS_CHARS)
                : analysisJson;
        sb.append(truncated);
        return sb.toString();
    }

    private List<SourceExplanationData> parse(String jsonResponse, List<Document> documents) {
        if (jsonResponse == null || jsonResponse.isBlank()) return List.of();
        String cleaned = stripMarkdownFence(jsonResponse.trim());

        try {
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode explanations = root.get("explanations");
            if (explanations == null || !explanations.isArray()) return List.of();

            Map<String, UUID> docIdByFilename = buildDocFilenameIndex(documents);

            List<SourceExplanationData> out = new ArrayList<>();
            Iterator<JsonNode> it = explanations.elements();
            while (it.hasNext()) {
                JsonNode node = it.next();
                String sourceKey = textOrNull(node, "sourceKey");
                String sourceType = textOrNull(node, "sourceType");
                String label = textOrNull(node, "label");
                String sentence = textOrNull(node, "sentence");
                String anchorDocName = textOrNull(node, "anchorDocName");

                if (sourceKey == null || label == null) continue;

                UUID anchorDocId = null;
                if ("DOCUMENT".equalsIgnoreCase(sourceType) && anchorDocName != null) {
                    anchorDocId = docIdByFilename.get(anchorDocName.toLowerCase(Locale.ROOT));
                }

                out.add(new SourceExplanationData(
                        sourceKey,
                        sourceType,
                        label,
                        sentence,
                        anchorDocId,
                        null,
                        null,
                        null
                ));
            }
            return out;
        } catch (Exception e) {
            log.warn("Failed to parse Haiku source explanations JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, UUID> buildDocFilenameIndex(List<Document> documents) {
        Map<String, UUID> index = new HashMap<>();
        if (documents == null) return index;
        for (Document doc : documents) {
            if (doc.getOriginalFilename() != null) {
                index.put(doc.getOriginalFilename().toLowerCase(Locale.ROOT), doc.getId());
            }
        }
        return index;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) return null;
        String s = child.asText();
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String stripMarkdownFence(String s) {
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline >= 0) s = s.substring(firstNewline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }
}
