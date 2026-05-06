package fr.ailegalcase.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseDeadline;
import fr.ailegalcase.casefile.CaseDeadlineRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.ImmigrationTitleDecision;
import fr.ailegalcase.casefile.ImmigrationTitleDecisionRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * F-193 SF-193-01 — Matérialise (et lit) l'alignement entre les points
 * procéduraux F-96 (statuts avocat) clonés sur une nouvelle analyse et les
 * sorties des outils décisionnels du dossier (Travail / Immigration / Famille
 * × FR + BE).
 *
 * <p>Pattern miroir {@link RetainedPisteAlignmentService} (F-192 SF-192-01).
 * Activation : appelé UNIQUEMENT depuis
 * {@code EnrichedAnalysisService.run} APRÈS
 * {@link RetainedPisteAlignmentService#materializeForAnalysis(CaseAnalysis)},
 * pour matérialiser :
 * <ol>
 *   <li>l'alignement (persisté dans {@code case_analyses.procedure_checks_alignment_json})</li>
 *   <li>les pieces_manquantes ({@code source = "PROCEDURE_CHECK_NON_COMPLIANT"})
 *       injectées dans le JSON {@code analysis_result} de la nouvelle analyse,
 *       idempotent</li>
 *   <li>les case_deadlines ({@code source = "PROCEDURE_CHECK_TO_CHECK"}) au
 *       niveau dossier pour les check TO_CHECK avec délai légal connu,
 *       idempotent sur la clé {@code (caseFileId, source, label)}</li>
 * </ol>
 *
 * <p>Strict gating Synthèse enrichie : aucun side-effect ailleurs.
 * {@link ProcedureCheckService#updateStatus} reste un PUT pur (cohérence F-96
 * stricte).</p>
 */
@Service
public class ProcedureCheckAlignmentService {

    private static final Logger log = LoggerFactory.getLogger(ProcedureCheckAlignmentService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<ProcedureCheckAlignment>> LIST_OF_ALIGNMENT =
            new TypeReference<>() {};

    /**
     * F-193 SF-193-01 — délais légaux statiques en mois pour les
     * {@code critereCode} dont le statut TO_CHECK justifie la création d'un
     * délai de vérification automatique. Sous-ensemble courant V1 — étendre
     * selon retour terrain (cf. mini-spec § Hors scope (d)).
     */
    private static final Map<String, Integer> CRITERE_CODE_DELAI_MOIS = buildDelaiMois();

    private static Map<String, Integer> buildDelaiMois() {
        Map<String, Integer> m = new HashMap<>();
        // F-DT-08 Validité licenciement — délai contestation prud'homal 12 mois (FR L.1471-1)
        m.put("FR_CONVOCATION", 12);
        m.put("FR_ENTRETIEN", 12);
        m.put("FR_DELAI_NOTIFICATION", 12);
        m.put("FR_MOTIVATION", 12);
        m.put("FR_MOTIF_REEL", 12);
        // F-DT-10 Rupture conventionnelle — délai de rétractation 15 jours = ~0.5 mois
        m.put("RC_DELAI_RETRACTATION", 1);
        m.put("RC_HOMOLOGATION", 1);
        // F-IM-06 Recours immigration — 30 jours = 1 mois
        m.put("IM06_RECOURS_TYPE", 1);
        // F-FA-07 Divorce amiable FR — délai de réflexion 15 jours = ~0.5 mois
        m.put("FR_DELAI_REFLEXION", 1);
        return Map.copyOf(m);
    }

    private final ProcedureCheckRepository procedureCheckRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseDeadlineRepository caseDeadlineRepository;
    private final ImmigrationTitleDecisionRepository titleDecisionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final Clock clock;

    @Autowired
    public ProcedureCheckAlignmentService(ProcedureCheckRepository procedureCheckRepository,
                                          CaseAnalysisRepository caseAnalysisRepository,
                                          CaseFileRepository caseFileRepository,
                                          CaseDeadlineRepository caseDeadlineRepository,
                                          ImmigrationTitleDecisionRepository titleDecisionRepository,
                                          WorkspaceMemberRepository workspaceMemberRepository,
                                          CurrentUserResolver currentUserResolver) {
        this(procedureCheckRepository, caseAnalysisRepository, caseFileRepository,
                caseDeadlineRepository, titleDecisionRepository, workspaceMemberRepository,
                currentUserResolver, Clock.systemUTC());
    }

    /** Constructeur de test (injection {@link Clock}). */
    ProcedureCheckAlignmentService(ProcedureCheckRepository procedureCheckRepository,
                                    CaseAnalysisRepository caseAnalysisRepository,
                                    CaseFileRepository caseFileRepository,
                                    CaseDeadlineRepository caseDeadlineRepository,
                                    ImmigrationTitleDecisionRepository titleDecisionRepository,
                                    WorkspaceMemberRepository workspaceMemberRepository,
                                    CurrentUserResolver currentUserResolver,
                                    Clock clock) {
        this.procedureCheckRepository = procedureCheckRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.caseDeadlineRepository = caseDeadlineRepository;
        this.titleDecisionRepository = titleDecisionRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.clock = clock;
    }

    // ====================================================================
    //  MATÉRIALISATION (appelée UNIQUEMENT depuis EnrichedAnalysisService.run
    //  APRÈS RetainedPisteAlignmentService.materializeForAnalysis F-192)
    // ====================================================================

    /**
     * Calcule + persiste l'alignement, propage les pieces_manquantes et délais
     * pour les checks F-96 (TO_CHECK / VERIFIED / NON_COMPLIANT) propagés sur
     * {@code newAnalysis}.
     *
     * <p>Fail-open : toute exception déclenche un log warn et laisse la nouvelle
     * analyse intacte (l'alignement reste {@code null}, mais le run de Synthèse
     * enrichie réussit quand même).</p>
     *
     * @param newAnalysis nouvelle analyse {@code DONE} créée par
     *                    {@code EnrichedAnalysisService.finalizeEnrichedAnalysis}
     */
    @Transactional
    public void materializeForAnalysis(CaseAnalysis newAnalysis) {
        if (newAnalysis == null || newAnalysis.getId() == null) return;
        try {
            List<ProcedureCheck> checks = procedureCheckRepository
                    .findByCaseAnalysisIdOrderByOrdreAsc(newAnalysis.getId());
            if (checks.isEmpty()) {
                // Rien à matérialiser — laisse procedure_checks_alignment_json à null
                return;
            }

            UUID caseFileId = newAnalysis.getCaseFile().getId();

            // (1) calcul de l'alignement
            List<ProcedureCheckAlignment> alignments = new ArrayList<>(checks.size());
            for (ProcedureCheck check : checks) {
                alignments.add(buildAlignment(check, caseFileId));
            }

            // (2) persiste le JSON sur la nouvelle analyse
            try {
                String json = MAPPER.writeValueAsString(alignments);
                newAnalysis.setProcedureChecksAlignmentJson(json);
                caseAnalysisRepository.save(newAnalysis);
            } catch (Exception e) {
                log.warn("F-193: failed to serialize procedure checks alignment for analysis {} — fail-open",
                        newAnalysis.getId(), e);
            }

            // (3) propagation pieces_manquantes (NON_COMPLIANT seulement)
            try {
                propagateToPiecesManquantes(newAnalysis, checks);
            } catch (Exception e) {
                log.warn("F-193: propagateToPiecesManquantes failed for analysis {} — fail-open",
                        newAnalysis.getId(), e);
            }

            // (4) propagation case_deadlines (TO_CHECK avec délai légal connu)
            try {
                propagateToDeadlines(newAnalysis.getCaseFile(), checks);
            } catch (Exception e) {
                log.warn("F-193: propagateToDeadlines failed for caseFile {} — fail-open",
                        newAnalysis.getCaseFile().getId(), e);
            }
        } catch (Exception e) {
            log.warn("F-193: materializeForAnalysis fail-open for analysis {}",
                    newAnalysis.getId(), e);
        }
    }

    // ---- alignment computation ----

    private ProcedureCheckAlignment buildAlignment(ProcedureCheck check, UUID caseFileId) {
        String toolId = ProcedureCheckToolMatcher.resolveToolId(check.getCritereCode());
        String matchStatus = computeMatchStatus(toolId, check, caseFileId);
        return new ProcedureCheckAlignment(
                check.getId(),
                check.getDescription(),
                check.getCritereCode(),
                check.getStatut() != null ? check.getStatut().name() : null,
                check.getExpectedValue(),
                check.getRaison(),
                toolId,
                matchStatus);
    }

    /**
     * Calcul du {@code matchStatus} :
     * <ul>
     *   <li>{@code NO_TARGET_TOOL} si pas de mapping</li>
     *   <li>{@code NON_COMPLIANT_FLAG} si statut F-96 = NON_COMPLIANT</li>
     *   <li>{@code TO_VERIFY_FLAG} si statut F-96 = TO_CHECK</li>
     *   <li>{@code VERIFIED} : confronte {@code expectedValue} à la sortie
     *       outil ({@code ALIGNED} / {@code DIVERGENT} / {@code NOT_ANALYZED})</li>
     * </ul>
     */
    private String computeMatchStatus(String toolId, ProcedureCheck check, UUID caseFileId) {
        if (toolId == null) return ProcedureCheckAlignment.STATUS_NO_TARGET_TOOL;

        ProcedureCheckStatus statut = check.getStatut();
        if (statut == ProcedureCheckStatus.NON_COMPLIANT) {
            return ProcedureCheckAlignment.STATUS_NON_COMPLIANT_FLAG;
        }
        if (statut == ProcedureCheckStatus.TO_CHECK) {
            return ProcedureCheckAlignment.STATUS_TO_VERIFY_FLAG;
        }
        // statut = VERIFIED : confronter expectedValue à la sortie outil
        try {
            return computeVerifiedMatchStatus(toolId, check, caseFileId);
        } catch (Exception e) {
            log.warn("F-193: tool {} read failed for check {} — NOT_ANALYZED (fail-open)",
                    toolId, check.getId(), e);
            return ProcedureCheckAlignment.STATUS_NOT_ANALYZED;
        }
    }

    /**
     * Pour un check VERIFIED, vérifie que l'outil cible a tourné et que sa
     * sortie matche {@code expectedValue}.
     *
     * <p>V1 : seul {@link ProcedureCheckToolMatcher#TOOL_IM_05_TITRE} pour
     * {@code IM05_MOTIF} bénéficie d'une comparaison fine ({@code motif} de
     * l'analyse {@link ImmigrationTitleDecision}). Pour les autres outils,
     * l'existence de l'analyse cible suffit pour {@code ALIGNED} (l'avocat a
     * déjà coché VERIFIED, on lui fait confiance ; les divergences fines sont
     * gérées par F-IA-03 séparément). {@code DIVERGENT} n'est produit que
     * pour les cas où la comparaison est faisable côté V1.</p>
     */
    private String computeVerifiedMatchStatus(String toolId, ProcedureCheck check, UUID caseFileId) {
        if (ProcedureCheckToolMatcher.TOOL_IM_05_TITRE.equals(toolId)) {
            return matchImmigrationTitleMotif(check, caseFileId);
        }
        // V1 : pour les autres outils, on traite VERIFIED comme ALIGNED par
        // défaut quand l'outil cible existe pour le dossier (au moins la
        // sortie outil est cohérente avec la décision avocat). Sinon NOT_ANALYZED.
        return outilTargetExists(toolId, caseFileId)
                ? ProcedureCheckAlignment.STATUS_ALIGNED
                : ProcedureCheckAlignment.STATUS_NOT_ANALYZED;
    }

    private String matchImmigrationTitleMotif(ProcedureCheck check, UUID caseFileId) {
        Optional<ImmigrationTitleDecision> decisionOpt = titleDecisionRepository.findByCaseFileId(caseFileId);
        if (decisionOpt.isEmpty()) return ProcedureCheckAlignment.STATUS_NOT_ANALYZED;

        ImmigrationTitleDecision decision = decisionOpt.get();
        String expected = check.getExpectedValue();
        if (expected == null || expected.isBlank()) {
            // VERIFIED sans expectedValue (binaire) — outil tourné = ALIGNED
            return ProcedureCheckAlignment.STATUS_ALIGNED;
        }
        String motif = decision.getMotif();
        if (motif == null) return ProcedureCheckAlignment.STATUS_DIVERGENT;
        return motif.trim().equalsIgnoreCase(expected.trim())
                ? ProcedureCheckAlignment.STATUS_ALIGNED
                : ProcedureCheckAlignment.STATUS_DIVERGENT;
    }

    /**
     * V1 minimal : pour les outils non-IM-05, on considère qu'un VERIFIED doit
     * être ALIGNED si l'outil cible existe sur le dossier ; sinon NOT_ANALYZED.
     * Cette méthode ne lit que le repo {@link ImmigrationTitleDecisionRepository}
     * (commun avec F-192) — pour les autres outils on reste fail-open et on
     * retourne {@code true} (ALIGNED par défaut, l'avocat a coché VERIFIED).
     *
     * <p>V2 : un dispatch outil par outil pourra être ajouté pour distinguer
     * NOT_ANALYZED finement. V1 reste optimiste pour ne pas bruiter sans
     * valeur ajoutée — F-IA-03 produit déjà des alertes de cohérence
     * spécifiques par outil.</p>
     */
    private boolean outilTargetExists(String toolId, UUID caseFileId) {
        // Seul F-IM-05 peut être vérifié ici (repo déjà injecté pour IM05_MOTIF).
        // Les autres : on assume l'outil disponible (ALIGNED par défaut V1).
        if (ProcedureCheckToolMatcher.TOOL_IM_05_TITRE.equals(toolId)) {
            return titleDecisionRepository.findByCaseFileId(caseFileId).isPresent();
        }
        return true;
    }

    // ---- propagation pieces_manquantes ----

    /**
     * Pour chaque check NON_COMPLIANT : insère son libellé dans
     * {@code analysis_result.pieces_manquantes} avec
     * {@code source = "PROCEDURE_CHECK_NON_COMPLIANT"} si non déjà présent
     * (matching {@code trim().toLowerCase()}). Idempotent.
     */
    private void propagateToPiecesManquantes(CaseAnalysis analysis, List<ProcedureCheck> checks) {
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

        Set<String> existingTexts = new HashSet<>();
        for (JsonNode p : pieces) {
            String t = extractPieceTexte(p);
            if (t != null) existingTexts.add(normalizeText(t));
        }

        boolean modified = false;
        for (ProcedureCheck check : checks) {
            if (check.getStatut() != ProcedureCheckStatus.NON_COMPLIANT) continue;
            String libelle = check.getDescription();
            if (libelle == null || libelle.isBlank()) continue;
            String norm = normalizeText(libelle);
            if (existingTexts.contains(norm)) continue;
            ObjectNode entry = MAPPER.createObjectNode();
            entry.put("texte", libelle.trim());
            entry.put("source", "PROCEDURE_CHECK_NON_COMPLIANT");
            entry.putNull("critere_code");
            pieces.add(entry);
            existingTexts.add(norm);
            modified = true;
        }

        if (modified) {
            try {
                analysis.setAnalysisResult(MAPPER.writeValueAsString(root));
                caseAnalysisRepository.save(analysis);
            } catch (Exception e) {
                log.warn("F-193: failed to write back analysis_result for analysis {} — fail-open",
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

    static String normalizeText(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }

    // ---- propagation case_deadlines ----

    /**
     * Pour chaque check TO_CHECK avec un {@code critereCode} dont le délai
     * légal est connu (cf. {@link #CRITERE_CODE_DELAI_MOIS}), crée une entrée
     * {@link CaseDeadline} {@code source = "PROCEDURE_CHECK_TO_CHECK"} idempotente
     * sur la clé {@code (caseFileId, source, label)}. Fail-open si pas de
     * délai connu (CA-10 V1 = sous-ensemble courant).
     */
    private void propagateToDeadlines(CaseFile caseFile, List<ProcedureCheck> checks) {
        if (caseFile == null) return;
        UUID caseFileId = caseFile.getId();

        List<CaseDeadline> existing = caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFileId);
        Set<String> existingKeys = new HashSet<>();
        for (CaseDeadline d : existing) {
            if ("PROCEDURE_CHECK_TO_CHECK".equals(d.getSource())) {
                existingKeys.add(d.getLabel());
            }
        }

        for (ProcedureCheck check : checks) {
            if (check.getStatut() != ProcedureCheckStatus.TO_CHECK) continue;
            String code = check.getCritereCode();
            if (code == null) continue;
            Integer nbMois = CRITERE_CODE_DELAI_MOIS.get(code.toUpperCase());
            if (nbMois == null) continue;

            String label = buildDeadlineLabel(check);
            if (existingKeys.contains(label)) continue;

            CaseDeadline d = new CaseDeadline();
            d.setCaseFile(caseFile);
            d.setLabel(label);
            d.setDueDate(LocalDate.now(clock).plusMonths(nbMois));
            d.setSource("PROCEDURE_CHECK_TO_CHECK");
            caseDeadlineRepository.save(d);
            existingKeys.add(label);
        }
    }

    static String buildDeadlineLabel(ProcedureCheck check) {
        String desc = check.getDescription() == null ? "" : check.getDescription().trim();
        // Limite la longueur pour respecter VARCHAR(255)
        String suffix = desc.length() <= 220 ? desc : desc.substring(0, 220) + "…";
        return "À vérifier : " + suffix;
    }

    // ====================================================================
    //  LECTURE (endpoint GET /procedure-checks-alignment)
    // ====================================================================

    /**
     * Renvoie la liste matérialisée pour la dernière analyse {@code DONE} du
     * dossier — pure lecture, aucun calcul à la volée. Isolation workspace
     * stricte (404 camouflage si dossier hors workspace).
     */
    @Transactional(readOnly = true)
    public List<ProcedureCheckAlignment> getForLatestAnalysis(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        Workspace workspace = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        return caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .map(a -> deserializeAlignment(a.getProcedureChecksAlignmentJson()))
                .orElseGet(List::of);
    }

    /**
     * Helper public pour les consommateurs ({@code CaseFileDashboardService},
     * tests). Désérialise le JSON persisté ; renvoie liste vide en cas
     * d'échec (fail-open).
     */
    public List<ProcedureCheckAlignment> deserializeAlignment(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<ProcedureCheckAlignment> list = MAPPER.readValue(json, LIST_OF_ALIGNMENT);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("F-193: failed to deserialize procedure_checks_alignment_json — empty list");
            return List.of();
        }
    }
}
