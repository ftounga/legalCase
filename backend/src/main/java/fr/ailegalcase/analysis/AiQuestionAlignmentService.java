package fr.ailegalcase.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * F-196 SF-196-01 — Matérialise (et lit) l'alignement entre les questions
 * complémentaires F-94 (avec leurs réponses avocat) et les pièces
 * {@code analysis_result.pieces_manquantes} déduites automatiquement par
 * keyword statique ({@link AiQuestionPieceExtractor}).
 *
 * <p>Pattern miroir {@link RetainedPisteAlignmentService} (F-192) +
 * {@link ProcedureCheckAlignmentService} (F-193) + {@link PieceManquanteAlignmentService}
 * (F-194) + {@link RisqueAlignmentService} (F-195).</p>
 *
 * <p>Activation : appelé UNIQUEMENT depuis {@code EnrichedAnalysisService.run}
 * APRÈS les hooks F-192/F-193/F-194/F-195. Matérialise :
 * <ol>
 *   <li>l'alignement (persisté dans {@code case_analyses.ai_questions_alignment_json})</li>
 *   <li>les pieces_manquantes nouvelles (sources {@code QUESTION_REPONDUE_OUI} /
 *       {@code QUESTION_REPONDUE_NON}) injectées dans le JSON
 *       {@code analysis_result.pieces_manquantes} de la nouvelle analyse,
 *       idempotent sur libellé normalisé</li>
 * </ol>
 *
 * <p>Cohérence F-94 STRICTE : les tables {@code ai_questions} et
 * {@code ai_question_answers} ne sont PAS modifiées par F-196. Le PUT réponse
 * ({@code AiQuestionAnswerCommandService.answer}) reste pur — aucun
 * side-effect immédiat sur pieces_manquantes (cf. test régression).</p>
 *
 * <p>Strict gating Synthèse enrichie : aucun side-effect ailleurs. Fail-open
 * (toute exception laisse la nouvelle analyse intacte).</p>
 */
@Service
public class AiQuestionAlignmentService {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionAlignmentService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<AiQuestionAlignment>> LIST_OF_ALIGNMENT =
            new TypeReference<>() {};

    /** Source PieceManquante quand la question "Avez-vous X ?" reçoit "oui". */
    static final String SOURCE_QUESTION_REPONDUE_OUI = "QUESTION_REPONDUE_OUI";
    /** Source PieceManquante quand la question "Avez-vous X ?" reçoit "non". */
    static final String SOURCE_QUESTION_REPONDUE_NON = "QUESTION_REPONDUE_NON";

    private final AiQuestionRepository aiQuestionRepository;
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public AiQuestionAlignmentService(AiQuestionRepository aiQuestionRepository,
                                       AiQuestionAnswerRepository aiQuestionAnswerRepository,
                                       CaseAnalysisRepository caseAnalysisRepository,
                                       CaseFileRepository caseFileRepository,
                                       WorkspaceMemberRepository workspaceMemberRepository,
                                       CurrentUserResolver currentUserResolver) {
        this.aiQuestionRepository = aiQuestionRepository;
        this.aiQuestionAnswerRepository = aiQuestionAnswerRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    // ====================================================================
    //  MATÉRIALISATION (appelée UNIQUEMENT depuis EnrichedAnalysisService.run
    //  APRÈS F-192/F-193/F-194/F-195)
    // ====================================================================

    /**
     * Calcule + persiste l'alignement (overlay JSON sur les questions F-94)
     * puis injecte les pieces_manquantes auto-déduites dans
     * {@code analysis_result}.
     *
     * <p>Cohérence F-94 STRICTE : ne mute PAS les tables
     * {@code ai_questions} / {@code ai_question_answers}.</p>
     *
     * <p>Fail-open : toute exception déclenche un log warn et laisse la
     * nouvelle analyse intacte (l'alignement reste {@code null}, mais le run
     * de Synthèse enrichie réussit quand même).</p>
     *
     * @param newAnalysis nouvelle analyse {@code DONE} créée par
     *                    {@code EnrichedAnalysisService.finalizeEnrichedAnalysis}
     */
    @Transactional
    public void materializeForAnalysis(CaseAnalysis newAnalysis) {
        if (newAnalysis == null || newAnalysis.getId() == null) return;
        try {
            UUID caseFileId = newAnalysis.getCaseFile() != null
                    ? newAnalysis.getCaseFile().getId() : null;
            if (caseFileId == null) return;

            // (1) charger toutes les questions du dossier (pas seulement de
            //     l'analyse en cours — F-94 produit des questions au niveau
            //     dossier, pas par analyse)
            List<AiQuestion> questions = aiQuestionRepository
                    .findByCaseFileIdOrderByOrderIndex(caseFileId);

            // (2) construire l'alignement question-par-question
            List<AiQuestionAlignment> alignments = new ArrayList<>(questions.size());
            for (AiQuestion q : questions) {
                String answerText = loadLatestAnswerText(q.getId());
                String pieceLibelle = AiQuestionPieceExtractor.extractPieceLibelle(q.getQuestionText());
                Boolean yesNo = AiQuestionPieceExtractor.parseYesNo(answerText);

                String statut;
                if (pieceLibelle == null) {
                    statut = AiQuestionAlignment.STATUT_INFO_ONLY;
                } else if (Boolean.TRUE.equals(yesNo)) {
                    statut = AiQuestionAlignment.STATUT_PIECE_OBTENUE;
                } else if (Boolean.FALSE.equals(yesNo)) {
                    statut = AiQuestionAlignment.STATUT_PIECE_MANQUANTE;
                } else {
                    statut = AiQuestionAlignment.STATUT_INFO_ONLY;
                }

                alignments.add(new AiQuestionAlignment(
                        q.getId(),
                        answerText,
                        pieceLibelle,
                        statut));
            }

            // (3) persiste le JSON sur la nouvelle analyse
            try {
                String json = MAPPER.writeValueAsString(alignments);
                newAnalysis.setAiQuestionsAlignmentJson(json);
                caseAnalysisRepository.save(newAnalysis);
            } catch (Exception e) {
                log.warn("F-196: failed to serialize ai questions alignment for analysis {} — fail-open",
                        newAnalysis.getId(), e);
            }

            // (4) propagation pieces_manquantes auto pour les alignements
            //     PIECE_OBTENUE / PIECE_MANQUANTE (idempotent sur libellé
            //     normalisé) — modifie le JSON analysis_result
            try {
                propagateToPiecesManquantes(newAnalysis, alignments);
            } catch (Exception e) {
                log.warn("F-196: propagateToPiecesManquantes failed for analysis {} — fail-open",
                        newAnalysis.getId(), e);
            }
        } catch (Exception e) {
            log.warn("F-196: materializeForAnalysis fail-open for analysis {}",
                    newAnalysis.getId(), e);
        }
    }

    /** Charge la dernière réponse texte de la question (ou {@code null}). */
    private String loadLatestAnswerText(UUID questionId) {
        try {
            return aiQuestionAnswerRepository
                    .findFirstByAiQuestionIdOrderByCreatedAtDesc(questionId)
                    .map(AiQuestionAnswer::getAnswerText)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // ---- propagation pieces_manquantes ----

    /**
     * Pour chaque alignement {@code PIECE_OBTENUE} / {@code PIECE_MANQUANTE}
     * dont le libellé n'est pas déjà dans
     * {@code analysis_result.pieces_manquantes}, ajoute une entrée idempotente
     * (clé : libellé normalisé). Source :
     * {@code QUESTION_REPONDUE_OUI} / {@code QUESTION_REPONDUE_NON}.
     */
    private void propagateToPiecesManquantes(CaseAnalysis analysis,
                                              List<AiQuestionAlignment> alignments) {
        String rawJson = analysis.getAnalysisResult();
        if (rawJson == null || rawJson.isBlank()) return;

        String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(rawJson);
        ObjectNode root;
        try {
            JsonNode node = MAPPER.readTree(stripped);
            if (!(node instanceof ObjectNode obj)) return;
            root = obj;
        } catch (Exception e) {
            return;
        }

        ArrayNode pieces;
        if (root.has("pieces_manquantes") && root.get("pieces_manquantes").isArray()) {
            pieces = (ArrayNode) root.get("pieces_manquantes");
        } else {
            pieces = MAPPER.createArrayNode();
            root.set("pieces_manquantes", pieces);
        }

        // Set des libellés existants (normalisé) pour idempotence
        Set<String> existing = new HashSet<>();
        for (JsonNode p : pieces) {
            String t = extractPieceTexte(p);
            if (t != null) existing.add(normalize(t));
        }

        boolean modified = false;
        for (AiQuestionAlignment a : alignments) {
            String pieceLibelle = a.pieceLibelleDeduit();
            if (pieceLibelle == null || pieceLibelle.isBlank()) continue;

            String source;
            if (AiQuestionAlignment.STATUT_PIECE_OBTENUE.equals(a.statutDeduction())) {
                source = SOURCE_QUESTION_REPONDUE_OUI;
            } else if (AiQuestionAlignment.STATUT_PIECE_MANQUANTE.equals(a.statutDeduction())) {
                source = SOURCE_QUESTION_REPONDUE_NON;
            } else {
                continue;
            }

            String norm = normalize(pieceLibelle);
            if (existing.contains(norm)) continue;

            ObjectNode entry = MAPPER.createObjectNode();
            entry.put("texte", pieceLibelle.trim());
            entry.put("source", source);
            entry.putNull("critere_code");
            pieces.add(entry);
            existing.add(norm);
            modified = true;
        }

        if (modified) {
            try {
                analysis.setAnalysisResult(MAPPER.writeValueAsString(root));
                caseAnalysisRepository.save(analysis);
            } catch (Exception e) {
                log.warn("F-196: failed to write back analysis_result for analysis {} — fail-open",
                        analysis.getId(), e);
            }
        }
    }

    private static String extractPieceTexte(JsonNode p) {
        if (p == null) return null;
        if (p.isTextual()) return p.asText();
        if (p.isObject()) {
            JsonNode t = p.get("texte");
            if (t != null && t.isTextual()) return t.asText();
        }
        return null;
    }

    static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }

    // ====================================================================
    //  LECTURE (endpoint GET /ai-questions-alignment)
    // ====================================================================

    /**
     * Renvoie la liste matérialisée pour la dernière analyse {@code DONE} du
     * dossier — pure lecture, aucun calcul à la volée. Isolation workspace
     * stricte (404 camouflage si dossier hors workspace).
     */
    @Transactional(readOnly = true)
    public List<AiQuestionAlignment> getForLatestAnalysis(UUID caseFileId,
                                                          OidcUser oidcUser,
                                                          Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        Workspace workspace = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        Optional<CaseAnalysis> latest = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE);
        return latest
                .map(a -> deserializeAlignment(a.getAiQuestionsAlignmentJson()))
                .orElseGet(List::of);
    }

    /**
     * Helper public pour les consommateurs ({@code CaseFileDashboardService},
     * tests). Désérialise le JSON persisté ; renvoie liste vide en cas
     * d'échec (fail-open).
     */
    public List<AiQuestionAlignment> deserializeAlignment(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<AiQuestionAlignment> list = MAPPER.readValue(json, LIST_OF_ALIGNMENT);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("F-196: failed to deserialize ai_questions_alignment_json — empty list");
            return List.of();
        }
    }
}
