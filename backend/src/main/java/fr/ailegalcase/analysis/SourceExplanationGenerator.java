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
    private final AiQuestionRepository aiQuestionRepository;
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private final ProcedureCheckRepository procedureCheckRepository;

    public SourceExplanationGenerator(AnthropicService anthropicService, ObjectMapper objectMapper,
                                      DocumentRepository documentRepository,
                                      AiQuestionRepository aiQuestionRepository,
                                      AiQuestionAnswerRepository aiQuestionAnswerRepository,
                                      ProcedureCheckRepository procedureCheckRepository) {
        this.anthropicService = anthropicService;
        this.objectMapper = objectMapper;
        this.documentRepository = documentRepository;
        this.aiQuestionRepository = aiQuestionRepository;
        this.aiQuestionAnswerRepository = aiQuestionAnswerRepository;
        this.procedureCheckRepository = procedureCheckRepository;
    }

    public List<SourceExplanationData> generate(CaseFile caseFile, CaseAnalysis analysis) {
        if (analysis == null || analysis.getAnalysisResult() == null || analysis.getAnalysisResult().isBlank()) {
            return List.of();
        }

        try {
            List<Document> documents = documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile);
            List<AiQuestion> questions = aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(analysis.getId());
            Map<UUID, String> answersByQuestionId = loadAnswers(questions);
            List<ProcedureCheck> f96Checks = procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId());
            List<String> missingPieces = extractMissingPieces(analysis.getAnalysisResult());

            String userMessage = buildUserMessage(documents, questions, answersByQuestionId, f96Checks, missingPieces, analysis.getAnalysisResult());
            String systemPrompt = buildSystemPrompt();
            AnthropicResult result = anthropicService.analyzeFast(systemPrompt, userMessage, MAX_TOKENS);
            return parse(result.content(), documents, questions, f96Checks, missingPieces);
        } catch (Exception e) {
            log.warn("SourceExplanationGenerator failed for case {}: {} — fallback to empty list",
                    caseFile.getId(), e.getMessage());
            return List.of();
        }
    }

    private Map<UUID, String> loadAnswers(List<AiQuestion> questions) {
        Map<UUID, String> answers = new HashMap<>();
        for (AiQuestion q : questions) {
            aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId())
                    .ifPresent(a -> answers.put(q.getId(), a.getAnswerText()));
        }
        return answers;
    }

    private List<String> extractMissingPieces(String analysisJson) {
        try {
            JsonNode root = objectMapper.readTree(analysisJson);
            JsonNode pieces = root.get("pieces_manquantes");
            if (pieces == null || !pieces.isArray()) return List.of();
            List<String> out = new ArrayList<>();
            for (JsonNode p : pieces) {
                if (p.isTextual()) {
                    out.add(p.asText());
                } else if (p.isObject() && p.has("texte")) {
                    out.add(p.get("texte").asText());
                }
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String buildSystemPrompt() {
        return """
                Tu es un assistant juridique qui reformule des données factuelles extraites d'un dossier
                en phrases pédagogiques pour un avocat, en citant précisément leur source.

                SÉPARATION STRICTE des 3 champs affichés dans le popover :

                - sentence (zone MOTIF) : phrase JURIDIQUE pure (≤ 220 car) qui décrit la règle ou le fait
                  détecté. NE DOIT PAS mentionner le nom du document, de la question, du point F-96, ou
                  de la pièce. Uniquement le QUOI.
                  Exemple CORRECT : "La convention BTP prévoit une prime d'ancienneté de 12 % après 15 ans."
                  Exemple INCORRECT : "Selon contrat_dupont.pdf, la prime est de 12 %."

                - label (zone SOURCE ligne 1) : nom affichable COURT et PROPRE de la source.
                  Exemple CORRECT DOCUMENT : "contrat_dupont.pdf"
                  Exemple CORRECT QUESTION_AI : "Quelle ancienneté ?"
                  Exemple CORRECT CHECKLIST_F96 : "FR_CONVOCATION"
                  Exemple CORRECT MISSING_PIECE : "Contrat de travail signé"

                - secondaryText (zone SOURCE ligne 2, ≤ 200 car) : extrait court ou détail spécifique
                  de la source — clause citée, extrait de réponse de l'avocat, raison F-96 tronquée,
                  intitulé précis de la pièce.
                  Exemple CORRECT : "Clause 6.2 — prime à partir de 15 ans d'ancienneté continue"
                  Exemple CORRECT : "Réponse avocat : 15 ans et 2 mois"
                  Exemple CORRECT : "F-96 marqué non conforme : pas de LRAR envoyée"

                - sourceType + anchor* : voir règles ci-dessous.

                Format JSON strict attendu, sans markdown, sans texte hors JSON :

                {
                  "explanations": [
                    {
                      "sourceKey": "convention_collective",
                      "sourceType": "DOCUMENT" | "QUESTION_AI" | "CHECKLIST_F96" | "CHAT" | "MISSING_PIECE" | "ANALYSIS_DETECTION",
                      "label": "contrat_dupont.pdf",
                      "sentence": "La convention BTP prévoit une prime de 12 % à 15 ans d'ancienneté.",
                      "secondaryText": "Clause 6.2 — prime d'ancienneté (CCN BTP IDCC 1596)",
                      "anchorDocName": "contrat_dupont.pdf",
                      "anchorQuestionId": null,
                      "anchorF96Code": null,
                      "anchorPieceIndex": null
                    }
                  ]
                }

                Règles de choix du sourceType et du anchor :

                - sourceType = DOCUMENT : la donnée est extraite d'un document précis du dossier.
                  Renseigne anchorDocName = nom EXACT d'un document de la liste "# Documents".
                - sourceType = QUESTION_AI : la donnée vient d'une réponse à une question IA.
                  Renseigne anchorQuestionId = id exact d'une question de la liste "# Questions".
                  secondaryText = extrait de la réponse de l'avocat.
                - sourceType = CHECKLIST_F96 : la donnée est alignée/contredite par un point procédural F-96.
                  Renseigne anchorF96Code = code exact de la liste "# Checklist F-96".
                  secondaryText = raison F-96 tronquée.
                - sourceType = MISSING_PIECE : la donnée est marquée manquante.
                  Renseigne anchorPieceIndex = index (0-based) de la liste "# Pièces manquantes".
                  secondaryText = intitulé de la pièce.
                - sourceType = ANALYSIS_DETECTION : la donnée est déduite de l'analyse globale sans
                  source unique identifiable. Aucun anchor.

                sourceKey en snake_case (génériques) OU code critère F96 en UPPER_CASE (spécifiques outil).
                Génériques : convention_collective, date_entree, salaire_brut_mensuel, conges_contractuels,
                prime_anciennete_contractuelle, type_rupture, date_licenciement, type_titre_sejour,
                type_recours, duree_mariage, revenus_conjoints, nationalite_ue, date_notification_decision_contestee.
                Codes F96 outil : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION,
                FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION,
                BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE,
                BE_INDEMNITE_MANIFESTE, DT09_TYPE_RUPTURE, RC_CONSENTEMENT, RC_DELAI_RETRACTATION,
                RC_HOMOLOGATION, RC_ASSISTANCE, RC_INDEMNITE, RC_ENTRETIENS, FA05_VALEUR_VENALE,
                FA05_CAPITAL_RESTANT, FA06_MODE_GARDE, IM05_MOTIF, IM06_RECOURS_TYPE, IM07_TITRE_TYPE,
                ainsi que codes étapes/pièces divorce FR_*/BE_*.

                Règles impératives :
                - Une seule entrée par sourceKey.
                - N'invente ni donnée, ni anchor : n'utilise que des IDs/codes/noms PRÉSENTS dans les listes fournies.
                - Préférer sourceType = DOCUMENT si un document explicite contient la donnée.
                - Préférer sourceType = QUESTION_AI / CHECKLIST_F96 si la donnée vient d'une réponse/point.
                - Produis uniquement les sourcekeys pour lesquels la synthèse contient une information
                  concrète — omet les autres (pas d'entrée vide).
                - Pas de conseils juridiques, uniquement du factuel reformulé. Rédige en français.
                """;
    }

    private String buildUserMessage(List<Document> documents, List<AiQuestion> questions,
                                    Map<UUID, String> answersByQuestionId, List<ProcedureCheck> f96Checks,
                                    List<String> missingPieces, String analysisJson) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Documents\n");
        if (documents == null || documents.isEmpty()) {
            sb.append("(aucun)\n");
        } else {
            for (Document doc : documents) {
                if (doc.getOriginalFilename() != null) {
                    sb.append("- ").append(doc.getOriginalFilename()).append('\n');
                }
            }
        }

        sb.append("\n# Questions\n");
        if (questions == null || questions.isEmpty()) {
            sb.append("(aucune)\n");
        } else {
            for (AiQuestion q : questions) {
                sb.append("- [id=").append(q.getId()).append("] ").append(q.getQuestionText());
                String ans = answersByQuestionId.get(q.getId());
                if (ans != null && !ans.isBlank()) {
                    sb.append(" → réponse : ").append(ans.length() > 160 ? ans.substring(0, 160) + "…" : ans);
                }
                sb.append('\n');
            }
        }

        sb.append("\n# Checklist F-96\n");
        if (f96Checks == null || f96Checks.isEmpty()) {
            sb.append("(aucun)\n");
        } else {
            for (ProcedureCheck c : f96Checks) {
                String code = c.getCritereCode();
                if (code == null || code.isBlank()) continue;
                sb.append("- [code=").append(code).append(", statut=").append(c.getStatut()).append("] ")
                        .append(c.getDescription() != null ? c.getDescription() : "");
                if (c.getRaison() != null && !c.getRaison().isBlank()) {
                    sb.append(" — raison : ").append(c.getRaison().length() > 160 ? c.getRaison().substring(0, 160) + "…" : c.getRaison());
                }
                sb.append('\n');
            }
        }

        sb.append("\n# Pièces manquantes\n");
        if (missingPieces == null || missingPieces.isEmpty()) {
            sb.append("(aucune)\n");
        } else {
            for (int i = 0; i < missingPieces.size(); i++) {
                sb.append("- [index=").append(i).append("] ").append(missingPieces.get(i)).append('\n');
            }
        }

        sb.append("\n# Synthèse IA (JSON)\n");
        String truncated = analysisJson.length() > MAX_SYNTHESIS_CHARS
                ? analysisJson.substring(0, MAX_SYNTHESIS_CHARS)
                : analysisJson;
        sb.append(truncated);
        return sb.toString();
    }

    private List<SourceExplanationData> parse(String jsonResponse, List<Document> documents,
                                              List<AiQuestion> questions, List<ProcedureCheck> f96Checks,
                                              List<String> missingPieces) {
        if (jsonResponse == null || jsonResponse.isBlank()) return List.of();
        String cleaned = stripMarkdownFence(jsonResponse.trim());

        try {
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode explanations = root.get("explanations");
            if (explanations == null || !explanations.isArray()) return List.of();

            Map<String, UUID> docIdByFilename = buildDocFilenameIndex(documents);
            java.util.Set<UUID> validQuestionIds = questions == null ? java.util.Set.of()
                    : questions.stream().map(AiQuestion::getId).collect(java.util.stream.Collectors.toSet());
            java.util.Set<String> validF96Codes = f96Checks == null ? java.util.Set.of()
                    : f96Checks.stream().map(ProcedureCheck::getCritereCode)
                            .filter(c -> c != null && !c.isBlank())
                            .collect(java.util.stream.Collectors.toSet());
            int piecesCount = missingPieces == null ? 0 : missingPieces.size();

            List<SourceExplanationData> out = new ArrayList<>();
            Iterator<JsonNode> it = explanations.elements();
            while (it.hasNext()) {
                JsonNode node = it.next();
                String sourceKey = textOrNull(node, "sourceKey");
                String sourceType = textOrNull(node, "sourceType");
                String label = textOrNull(node, "label");
                String sentence = textOrNull(node, "sentence");
                String secondaryText = textOrNull(node, "secondaryText");

                if (sourceKey == null || label == null) continue;

                UUID anchorDocId = null;
                UUID anchorQuestionId = null;
                String anchorF96Code = null;
                Integer anchorPieceIndex = null;

                if ("DOCUMENT".equalsIgnoreCase(sourceType)) {
                    String anchorDocName = textOrNull(node, "anchorDocName");
                    if (anchorDocName != null) {
                        anchorDocId = docIdByFilename.get(anchorDocName.toLowerCase(Locale.ROOT));
                    }
                } else if ("QUESTION_AI".equalsIgnoreCase(sourceType)) {
                    String raw = textOrNull(node, "anchorQuestionId");
                    if (raw != null) {
                        try {
                            UUID qid = UUID.fromString(raw);
                            if (validQuestionIds.contains(qid)) anchorQuestionId = qid;
                        } catch (IllegalArgumentException ignored) { /* anchor null */ }
                    }
                } else if ("CHECKLIST_F96".equalsIgnoreCase(sourceType)) {
                    String code = textOrNull(node, "anchorF96Code");
                    if (code != null && validF96Codes.contains(code)) anchorF96Code = code;
                } else if ("MISSING_PIECE".equalsIgnoreCase(sourceType)) {
                    JsonNode idx = node.get("anchorPieceIndex");
                    if (idx != null && idx.isInt()) {
                        int v = idx.asInt();
                        if (v >= 0 && v < piecesCount) anchorPieceIndex = v;
                    }
                }

                out.add(new SourceExplanationData(
                        sourceKey,
                        sourceType,
                        label,
                        sentence,
                        secondaryText,
                        anchorDocId,
                        anchorQuestionId,
                        anchorF96Code,
                        null,
                        anchorPieceIndex
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
