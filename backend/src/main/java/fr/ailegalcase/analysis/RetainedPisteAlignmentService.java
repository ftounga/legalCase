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
import fr.ailegalcase.casefile.ImmigrationRecours;
import fr.ailegalcase.casefile.ImmigrationRecoursRepository;
import fr.ailegalcase.casefile.ImmigrationTitleDecision;
import fr.ailegalcase.casefile.ImmigrationTitleDecisionRepository;
import fr.ailegalcase.casefile.TitleRecommendation;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-192 SF-192-01 — Matérialise (et lit) l'alignement entre les pistes
 * stratégiques RETAINED clonées sur une nouvelle analyse et les sorties des
 * outils décisionnels du dossier (TITRE / RECOURS Immigration FR + BE).
 *
 * <p>Activation : appelé UNIQUEMENT depuis
 * {@code EnrichedAnalysisService.run} après
 * {@code propagateRetainedAndDiscarded}, pour matérialiser :
 * <ol>
 *   <li>l'alignement (persisté dans {@code case_analyses.retained_pistes_alignment_json})</li>
 *   <li>les pieces_manquantes ({@code source = "PISTE_RETENUE"}) injectées dans
 *       le JSON {@code analysis_result} de la nouvelle analyse, idempotent</li>
 *   <li>les case_deadlines ({@code source = "PISTE_RETENUE"}) au niveau dossier,
 *       idempotent sur la clé {@code (caseFileId, source, label)}</li>
 * </ol>
 *
 * <p>Strict gating Synthèse enrichie : aucune side-effect ailleurs.
 * {@link StrategicOptionService#updateStatus} reste un PUT pur (cohérence F-176).</p>
 */
@Service
public class RetainedPisteAlignmentService {

    private static final Logger log = LoggerFactory.getLogger(RetainedPisteAlignmentService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<RetainedPisteAlignment>> LIST_OF_ALIGNMENT =
            new TypeReference<>() {};

    /** Regex F-192 SF-192-01 : extrait un nombre + unité {mois|an|ans} du horizon temporel. */
    private static final Pattern HORIZON_REGEX = Pattern.compile(
            "(?i)(\\d+)\\s*(mois|ans?|an)");

    private final StrategicOptionRepository strategicOptionRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseDeadlineRepository caseDeadlineRepository;
    private final ImmigrationTitleDecisionRepository titleDecisionRepository;
    private final ImmigrationRecoursRepository recoursRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final Clock clock;

    @Autowired
    public RetainedPisteAlignmentService(StrategicOptionRepository strategicOptionRepository,
                                          CaseAnalysisRepository caseAnalysisRepository,
                                          CaseFileRepository caseFileRepository,
                                          CaseDeadlineRepository caseDeadlineRepository,
                                          ImmigrationTitleDecisionRepository titleDecisionRepository,
                                          ImmigrationRecoursRepository recoursRepository,
                                          WorkspaceMemberRepository workspaceMemberRepository,
                                          CurrentUserResolver currentUserResolver) {
        this(strategicOptionRepository, caseAnalysisRepository, caseFileRepository,
                caseDeadlineRepository, titleDecisionRepository, recoursRepository,
                workspaceMemberRepository, currentUserResolver, Clock.systemUTC());
    }

    /** Constructeur de test (injection {@link Clock}). */
    RetainedPisteAlignmentService(StrategicOptionRepository strategicOptionRepository,
                                   CaseAnalysisRepository caseAnalysisRepository,
                                   CaseFileRepository caseFileRepository,
                                   CaseDeadlineRepository caseDeadlineRepository,
                                   ImmigrationTitleDecisionRepository titleDecisionRepository,
                                   ImmigrationRecoursRepository recoursRepository,
                                   WorkspaceMemberRepository workspaceMemberRepository,
                                   CurrentUserResolver currentUserResolver,
                                   Clock clock) {
        this.strategicOptionRepository = strategicOptionRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.caseDeadlineRepository = caseDeadlineRepository;
        this.titleDecisionRepository = titleDecisionRepository;
        this.recoursRepository = recoursRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.clock = clock;
    }

    // ====================================================================
    //  MATÉRIALISATION (appelée UNIQUEMENT depuis EnrichedAnalysisService.run)
    // ====================================================================

    /**
     * Calcule + persiste l'alignement, propage les pieces_manquantes et délais
     * pour les pistes RETAINED clonées sur {@code newAnalysis}.
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
            List<StrategicOption> retained = strategicOptionRepository
                    .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                            newAnalysis.getId(), StrategicOptionStatus.RETAINED);
            if (retained.isEmpty()) {
                // Rien à matérialiser — laisse retained_pistes_alignment_json à null
                return;
            }

            UUID caseFileId = newAnalysis.getCaseFile().getId();

            // (1) calcul de l'alignement
            List<RetainedPisteAlignment> alignments = new ArrayList<>(retained.size());
            for (StrategicOption piste : retained) {
                alignments.add(buildAlignment(piste, caseFileId));
            }

            // (2) persiste le JSON sur la nouvelle analyse
            try {
                String json = MAPPER.writeValueAsString(alignments);
                newAnalysis.setRetainedPistesAlignmentJson(json);
                caseAnalysisRepository.save(newAnalysis);
            } catch (Exception e) {
                log.warn("F-192: failed to serialize retained pistes alignment for analysis {} — fail-open",
                        newAnalysis.getId(), e);
            }

            // (3) propagation pieces_manquantes (modifie le JSON analysis_result)
            try {
                propagateToPiecesManquantes(newAnalysis, retained);
            } catch (Exception e) {
                log.warn("F-192: propagateToPiecesManquantes failed for analysis {} — fail-open",
                        newAnalysis.getId(), e);
            }

            // (4) propagation case_deadlines (idempotent niveau dossier)
            try {
                propagateToDeadlines(newAnalysis.getCaseFile(), retained);
            } catch (Exception e) {
                log.warn("F-192: propagateToDeadlines failed for caseFile {} — fail-open",
                        newAnalysis.getCaseFile().getId(), e);
            }
        } catch (Exception e) {
            log.warn("F-192: materializeForAnalysis fail-open for analysis {}",
                    newAnalysis.getId(), e);
        }
    }

    // ---- alignment computation ----

    private RetainedPisteAlignment buildAlignment(StrategicOption piste, UUID caseFileId) {
        String toolId = RetainedPisteToolMatcher.resolveToolId(piste.getTexte(), piste.getBaseJuridique());
        String matchStatus;
        if (toolId == null) {
            matchStatus = RetainedPisteAlignment.STATUS_NO_TARGET_TOOL;
        } else {
            matchStatus = computeMatchStatus(toolId, piste, caseFileId);
        }
        return new RetainedPisteAlignment(
                piste.getId(),
                piste.getTexte(),
                piste.getBaseJuridique(),
                piste.getHorizonTemporel(),
                deserializeConditions(piste.getConditionsJson()),
                toolId,
                matchStatus);
    }

    private String computeMatchStatus(String toolId, StrategicOption piste, UUID caseFileId) {
        try {
            return switch (toolId) {
                case RetainedPisteToolMatcher.TOOL_TITRE -> matchTitre(piste, caseFileId);
                case RetainedPisteToolMatcher.TOOL_RECOURS -> matchRecours(piste, caseFileId);
                default -> RetainedPisteAlignment.STATUS_NOT_ANALYZED;
            };
        } catch (Exception e) {
            log.warn("F-192: tool {} read failed for piste {} — NOT_ANALYZED (fail-open)",
                    toolId, piste.getId(), e);
            return RetainedPisteAlignment.STATUS_NOT_ANALYZED;
        }
    }

    private String matchTitre(StrategicOption piste, UUID caseFileId) {
        ImmigrationTitleDecision decision = titleDecisionRepository.findByCaseFileId(caseFileId).orElse(null);
        if (decision == null) return RetainedPisteAlignment.STATUS_NOT_ANALYZED;

        String json = decision.getRecommendedTitles();
        if (json == null || json.isBlank()) return RetainedPisteAlignment.STATUS_DIVERGENT;

        List<TitleRecommendation> recommendations;
        try {
            recommendations = MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, TitleRecommendation.class));
        } catch (Exception e) {
            return RetainedPisteAlignment.STATUS_DIVERGENT;
        }
        if (recommendations == null || recommendations.isEmpty()) {
            return RetainedPisteAlignment.STATUS_DIVERGENT;
        }
        // Top-3
        int max = Math.min(3, recommendations.size());
        List<TitleRecommendation> top3 = recommendations.subList(0, max);

        for (TitleRecommendation rec : top3) {
            if (matchesTitre(piste, rec)) {
                return RetainedPisteAlignment.STATUS_ALIGNED;
            }
        }
        return RetainedPisteAlignment.STATUS_DIVERGENT;
    }

    private static boolean matchesTitre(StrategicOption piste, TitleRecommendation rec) {
        String code = rec.code() == null ? "" : rec.code();
        String label = rec.label() == null ? "" : rec.label();
        String base = piste.getBaseJuridique() == null ? "" : piste.getBaseJuridique();
        String texte = piste.getTexte() == null ? "" : piste.getTexte();

        // Numéros d'article comme "L.421-14" → vérifier dans label/conditions de la reco
        Matcher articleMatcher = Pattern.compile("(?i)L\\.?\\s*(\\d+(?:-\\d+)*)").matcher(base);
        while (articleMatcher.find()) {
            String num = articleMatcher.group(1);
            if (label.contains(num)) return true;
            if (rec.conditions() != null && rec.conditions().contains(num)) return true;
        }

        // Code (ex. PASSEPORT_TALENT_CHERCHEUR) ↔ keyword texte
        String codeNormalized = code.replace('_', ' ').toLowerCase();
        if (!codeNormalized.isBlank() && texte.toLowerCase().contains(codeNormalized)) return true;

        // Label exact dans le texte
        if (!label.isBlank() && texte.toLowerCase().contains(label.toLowerCase())) return true;

        return false;
    }

    private String matchRecours(StrategicOption piste, UUID caseFileId) {
        ImmigrationRecours recours = recoursRepository.findByCaseFileId(caseFileId).orElse(null);
        if (recours == null) return RetainedPisteAlignment.STATUS_NOT_ANALYZED;

        String type = recours.getRecoursType();
        if (type == null || type.isBlank()) return RetainedPisteAlignment.STATUS_DIVERGENT;

        String texte = piste.getTexte() == null ? "" : piste.getTexte().toUpperCase();
        String base = piste.getBaseJuridique() == null ? "" : piste.getBaseJuridique().toUpperCase();
        String typeUp = type.toUpperCase();

        // Match exact code (ex. RECOURS_GRACIEUX_PREFET)
        if (texte.contains(typeUp) || base.contains(typeUp)) return RetainedPisteAlignment.STATUS_ALIGNED;

        // Match keyword par famille de recours
        if (typeUp.contains("GRACIEUX") && (texte.contains("GRACIEUX") || texte.contains("RAPO"))) {
            return RetainedPisteAlignment.STATUS_ALIGNED;
        }
        if (typeUp.contains("CONTENTIEUX") && (texte.contains("CONTENTIEUX") || texte.contains("REP")
                || texte.contains("TRIBUNAL ADMINISTRATIF"))) {
            return RetainedPisteAlignment.STATUS_ALIGNED;
        }
        if (typeUp.contains("CNDA") && texte.contains("CNDA")) return RetainedPisteAlignment.STATUS_ALIGNED;
        if (typeUp.contains("CGRA") && texte.contains("CGRA")) return RetainedPisteAlignment.STATUS_ALIGNED;
        if (typeUp.contains("CCE") && texte.contains("CCE")) return RetainedPisteAlignment.STATUS_ALIGNED;

        return RetainedPisteAlignment.STATUS_DIVERGENT;
    }

    // ---- propagation pieces_manquantes ----

    private void propagateToPiecesManquantes(CaseAnalysis analysis, List<StrategicOption> retainedPistes) {
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

        // Set des textes existants (normalisé) pour idempotence
        Set<String> existingTexts = new HashSet<>();
        for (JsonNode p : pieces) {
            String t = extractPieceTexte(p);
            if (t != null) existingTexts.add(normalizeText(t));
        }

        boolean modified = false;
        for (StrategicOption piste : retainedPistes) {
            List<String> conditions = deserializeConditions(piste.getConditionsJson());
            for (String cond : conditions) {
                if (cond == null || cond.isBlank()) continue;
                String norm = normalizeText(cond);
                if (existingTexts.contains(norm)) continue;
                ObjectNode entry = MAPPER.createObjectNode();
                entry.put("texte", cond.trim());
                entry.put("source", "PISTE_RETENUE");
                entry.putNull("critere_code");
                pieces.add(entry);
                existingTexts.add(norm);
                modified = true;
            }
        }

        if (modified) {
            try {
                analysis.setAnalysisResult(MAPPER.writeValueAsString(root));
                caseAnalysisRepository.save(analysis);
            } catch (Exception e) {
                log.warn("F-192: failed to write back analysis_result for analysis {} — fail-open",
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

    private void propagateToDeadlines(CaseFile caseFile, List<StrategicOption> retainedPistes) {
        if (caseFile == null) return;
        UUID caseFileId = caseFile.getId();

        // Charge les délais existants pour le dossier (clé d'unicité = source + label)
        List<CaseDeadline> existing = caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFileId);
        Set<String> existingKeys = new HashSet<>();
        for (CaseDeadline d : existing) {
            if ("PISTE_RETENUE".equals(d.getSource())) {
                existingKeys.add(d.getLabel());
            }
        }

        for (StrategicOption piste : retainedPistes) {
            String horizon = piste.getHorizonTemporel();
            if (horizon == null || horizon.isBlank()) continue;

            Integer nbMois = parseHorizonMonths(horizon);
            if (nbMois == null) continue;

            String label = buildDeadlineLabel(piste);
            if (existingKeys.contains(label)) continue;

            CaseDeadline d = new CaseDeadline();
            d.setCaseFile(caseFile);
            d.setLabel(label);
            d.setDueDate(LocalDate.now(clock).plusMonths(nbMois));
            d.setSource("PISTE_RETENUE");
            caseDeadlineRepository.save(d);
            existingKeys.add(label);
        }
    }

    /**
     * Parse {@code "Court terme (3-6 mois)"} → 6 (la borne haute), {@code "2 ans"} → 24.
     * Retourne {@code null} si non parsable.
     */
    static Integer parseHorizonMonths(String horizon) {
        if (horizon == null) return null;
        Matcher m = HORIZON_REGEX.matcher(horizon);
        Integer last = null;
        while (m.find()) {
            int n;
            try {
                n = Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                continue;
            }
            String unit = m.group(2).toLowerCase();
            int months = unit.startsWith("an") ? n * 12 : n;
            // garde la dernière occurrence (= borne haute typique "3-6 mois")
            last = months;
        }
        return last;
    }

    static String buildDeadlineLabel(StrategicOption piste) {
        String texte = piste.getTexte() == null ? "" : piste.getTexte().trim();
        // Limite la longueur pour respecter VARCHAR(255)
        String suffix = texte.length() <= 230 ? texte : texte.substring(0, 230) + "…";
        return "Stratégie : " + suffix;
    }

    // ====================================================================
    //  LECTURE (endpoint GET /retained-pistes-alignment)
    // ====================================================================

    /**
     * Renvoie la liste matérialisée pour la dernière analyse {@code DONE} du dossier
     * — pure lecture, aucun calcul à la volée. Isolation workspace stricte (404
     * camouflage si dossier hors workspace).
     */
    @Transactional(readOnly = true)
    public List<RetainedPisteAlignment> getForLatestAnalysis(UUID caseFileId, OidcUser oidcUser, Principal principal) {
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
                .map(a -> deserializeAlignment(a.getRetainedPistesAlignmentJson()))
                .orElseGet(List::of);
    }

    /** Helper public pour les consommateurs ({@code CaseFileDashboardService}, tests). */
    public List<RetainedPisteAlignment> deserializeAlignment(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<RetainedPisteAlignment> list = MAPPER.readValue(json, LIST_OF_ALIGNMENT);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("F-192: failed to deserialize retained_pistes_alignment_json — empty list");
            return List.of();
        }
    }

    // ---- helpers ----

    private static List<String> deserializeConditions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> list = MAPPER.readValue(json, new TypeReference<List<String>>() {});
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }
}
