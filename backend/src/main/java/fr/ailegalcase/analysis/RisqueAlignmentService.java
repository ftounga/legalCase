package fr.ailegalcase.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * F-195 SF-195-01 — Matérialise (et lit) l'alignement entre les risques
 * extraits par l'IA dans {@code analysis_result.risques} et les statuts avocat
 * curés dans {@link RisqueStatus}, et recompute un {@code score_risque_avocat}
 * parallèle excluant les ÉCARTÉ.
 *
 * <p>Pattern miroir {@link PieceManquanteAlignmentService} (F-194). Activation :
 * appelé UNIQUEMENT depuis {@code EnrichedAnalysisService.run} APRÈS les hooks
 * F-192, F-193 et F-194 pour matérialiser :
 * <ol>
 *   <li>l'alignement (persisté dans {@code case_analyses.risques_alignment_json})</li>
 *   <li>le score recomputé excluant ÉCARTÉ (persisté dans
 *       {@code case_analyses.score_risque_avocat_json})</li>
 * </ol>
 *
 * <p>Cohérence F-IA-02 STRICTE : le JSON {@code risques} dans
 * {@code analysis_result} N'EST PAS muté par F-195, et le {@code score_risque}
 * IA brut reste inchangé. F-195 produit un score parallèle visible côté avocat
 * pour transparence.</p>
 *
 * <p>Strict gating Synthèse enrichie : aucun side-effect ailleurs.
 * {@link RisqueStatusService#upsertStatus} reste un PUT pur (test régression
 * obligatoire).</p>
 */
@Service
public class RisqueAlignmentService {

    private static final Logger log = LoggerFactory.getLogger(RisqueAlignmentService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<RisqueAlignment>> LIST_OF_ALIGNMENT =
            new TypeReference<>() {};

    private final RisqueStatusRepository risqueStatusRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public RisqueAlignmentService(RisqueStatusRepository risqueStatusRepository,
                                   CaseAnalysisRepository caseAnalysisRepository,
                                   CaseFileRepository caseFileRepository,
                                   WorkspaceMemberRepository workspaceMemberRepository,
                                   CurrentUserResolver currentUserResolver) {
        this.risqueStatusRepository = risqueStatusRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    // ====================================================================
    //  MATÉRIALISATION (appelée UNIQUEMENT depuis EnrichedAnalysisService.run
    //  APRÈS RetainedPisteAlignmentService + ProcedureCheckAlignmentService
    //  + PieceManquanteAlignmentService)
    // ====================================================================

    /**
     * Calcule + persiste l'alignement (overlay statut sur le JSON risques IA)
     * puis recompute un {@code score_risque_avocat} parallèle excluant les
     * risques ÉCARTÉ — stocké dans une colonne séparée pour préserver F-IA-02
     * strict (le score IA brut reste inchangé).
     *
     * <p>Cohérence F-IA-02 STRICTE : ne mute PAS le JSON
     * {@code analysis_result.risques} ni le {@code score_risque} IA brut.</p>
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
            UUID caseFileId = newAnalysis.getCaseFile().getId();

            // (1) extraire la liste risques du JSON IA (lecture seule)
            List<String> risquesIa = extractRisquesFromAnalysisResult(newAnalysis.getAnalysisResult());

            // (2) charger l'overlay statut avocat
            List<RisqueStatus> statusesAll =
                    risqueStatusRepository.findByCaseFileId(caseFileId);
            Map<String, RisqueStatus> statusByNorm = new HashMap<>();
            for (RisqueStatus s : statusesAll) {
                if (s.getRisqueLibelleNormalise() != null) {
                    statusByNorm.put(s.getRisqueLibelleNormalise(), s);
                }
            }

            // (3) construire l'alignement : (a) chaque risque IA + statut overlay
            //     (défaut A_CREUSER) avec mapping toolIds, (b) ajouter les risques
            //     overlay statut VALIDE / ECARTE qui ne sont plus dans le JSON IA
            //     (l'IA a régénéré sans les mentionner) — utile pour mémoire
            //     dashboard.
            List<RisqueAlignment> alignments = new ArrayList<>();
            // dédup sur libellé normalisé
            LinkedHashMap<String, RisqueAlignment> dedup = new LinkedHashMap<>();
            for (String risque : risquesIa) {
                if (risque == null || risque.isBlank()) continue;
                String norm = normalize(risque);
                if (dedup.containsKey(norm)) continue;
                RisqueStatus overlay = statusByNorm.get(norm);
                String statut = overlay != null ? overlay.getStatut()
                        : RisqueStatus.STATUT_A_CREUSER;
                String raison = overlay != null ? overlay.getRaisonEcarte() : null;
                List<String> toolIds = RisqueToolMatcher.resolveToolIds(risque);
                dedup.put(norm, new RisqueAlignment(risque.trim(), statut, raison, toolIds));
            }
            // ajouter les risques overlay non encore dans dedup (statut VALIDE / ECARTE)
            for (RisqueStatus s : statusesAll) {
                String norm = s.getRisqueLibelleNormalise();
                if (norm == null || dedup.containsKey(norm)) continue;
                if (RisqueStatus.STATUT_VALIDE.equals(s.getStatut())
                        || RisqueStatus.STATUT_ECARTE.equals(s.getStatut())) {
                    List<String> toolIds = RisqueToolMatcher.resolveToolIds(s.getRisqueLibelleOriginal());
                    dedup.put(norm, new RisqueAlignment(
                            s.getRisqueLibelleOriginal(), s.getStatut(),
                            s.getRaisonEcarte(), toolIds));
                }
            }
            alignments.addAll(dedup.values());

            // (4) persiste le JSON sur la nouvelle analyse — sans toucher analysis_result
            try {
                String json = MAPPER.writeValueAsString(alignments);
                newAnalysis.setRisquesAlignmentJson(json);
            } catch (Exception e) {
                log.warn("F-195: failed to serialize risques alignment for analysis {} — fail-open",
                        newAnalysis.getId(), e);
            }

            // (5) recompute score_risque_avocat (exclut ÉCARTÉ)
            try {
                String scoreAvocatJson = computeScoreAvocat(newAnalysis.getAnalysisResult(),
                        newAnalysis.getRiskScore(), newAnalysis.getRiskLevel(), alignments);
                newAnalysis.setScoreRisqueAvocatJson(scoreAvocatJson);
            } catch (Exception e) {
                log.warn("F-195: failed to recompute score_risque_avocat for analysis {} — fail-open",
                        newAnalysis.getId(), e);
            }

            caseAnalysisRepository.save(newAnalysis);
        } catch (Exception e) {
            log.warn("F-195: materializeForAnalysis fail-open for analysis {}",
                    newAnalysis.getId(), e);
        }
    }

    // ---- extraction depuis analysis_result ----

    /** Extrait les libellés risques du JSON {@code analysis_result}. */
    static List<String> extractRisquesFromAnalysisResult(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return List.of();
        try {
            String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(rawJson);
            JsonNode root = MAPPER.readTree(stripped);
            JsonNode risques = root.get("risques");
            if (risques == null || !risques.isArray()) return List.of();
            List<String> out = new ArrayList<>();
            for (JsonNode r : risques) {
                String t = extractRisqueTexte(r);
                if (t != null && !t.isBlank()) out.add(t);
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String extractRisqueTexte(JsonNode r) {
        if (r == null) return null;
        if (r.isTextual()) return r.asText();
        if (r.isObject()) {
            JsonNode t = r.get("texte");
            if (t != null && t.isTextual()) return t.asText();
        }
        return null;
    }

    static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }

    // ---- score_risque_avocat ----

    /**
     * Recompute un score risque avocat parallèle excluant les ÉCARTÉ.
     *
     * <p>Stratégie V1 : si IA produit un score initial (niveau + valeur),
     * appliquer un facteur de réduction proportionnel au nombre de risques
     * écartés. Si tous les risques sont écartés, le score retombe à 0/FAIBLE.
     * Si aucun risque écarté, le score reste identique au score IA brut.</p>
     *
     * <p>Format JSON : {@code {"niveau":"FAIBLE|MOYEN|ELEVE","valeur":N,
     * "totalRisques":N,"risquesEcartes":N,"risquesValides":N,"risquesACreuser":N,
     * "scoreIaBrut":N,"niveauIaBrut":"..."}} — l'avocat voit les 2 valeurs.</p>
     *
     * @return JSON sérialisé du score avocat, ou {@code null} si l'IA n'a pas
     *         fourni de score initial
     */
    static String computeScoreAvocat(String analysisResultJson, Integer scoreIaBrut,
                                      String niveauIaBrut, List<RisqueAlignment> alignments) {
        if (alignments == null) alignments = List.of();
        int total = alignments.size();
        int valides = 0, ecartes = 0, aCreuser = 0;
        for (RisqueAlignment a : alignments) {
            if (RisqueStatus.STATUT_VALIDE.equals(a.statut())) valides++;
            else if (RisqueStatus.STATUT_ECARTE.equals(a.statut())) ecartes++;
            else aCreuser++;
        }

        // Score recomputé : facteur de réduction proportionnel aux ÉCARTÉ
        Integer scoreAvocat = null;
        String niveauAvocat = null;
        if (scoreIaBrut != null && total > 0) {
            int retenus = total - ecartes; // VALIDE + A_CREUSER
            if (retenus <= 0) {
                scoreAvocat = 0;
            } else {
                // Proportion conservée : score brut * (retenus / total)
                scoreAvocat = (int) Math.round(((double) scoreIaBrut) * retenus / total);
            }
            niveauAvocat = niveauFromScore(scoreAvocat);
        } else if (scoreIaBrut != null) {
            // Aucun risque dans l'analyse ⇒ score avocat = score IA (rien à exclure)
            scoreAvocat = scoreIaBrut;
            niveauAvocat = niveauIaBrut;
        }

        try {
            ObjectNode node = MAPPER.createObjectNode();
            if (niveauAvocat != null) node.put("niveau", niveauAvocat);
            if (scoreAvocat != null) node.put("valeur", scoreAvocat);
            node.put("totalRisques", total);
            node.put("risquesValides", valides);
            node.put("risquesEcartes", ecartes);
            node.put("risquesACreuser", aCreuser);
            if (scoreIaBrut != null) node.put("scoreIaBrut", scoreIaBrut);
            if (niveauIaBrut != null) node.put("niveauIaBrut", niveauIaBrut);
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("F-195: failed to serialize score_risque_avocat — fail-open");
            return null;
        }
    }

    /** Mapping valeur (0-100) → niveau FAIBLE/MOYEN/ELEVE. Cohérent F-IA-02. */
    private static String niveauFromScore(int v) {
        if (v >= 67) return "ELEVE";
        if (v >= 34) return "MOYEN";
        return "FAIBLE";
    }

    // ====================================================================
    //  LECTURE (endpoint GET /risques-alignment)
    // ====================================================================

    /**
     * Renvoie la liste matérialisée pour la dernière analyse {@code DONE} du
     * dossier — pure lecture, aucun calcul à la volée. Isolation workspace
     * stricte (404 camouflage si dossier hors workspace).
     */
    @Transactional(readOnly = true)
    public List<RisqueAlignment> getForLatestAnalysis(UUID caseFileId,
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

        return caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .map(a -> deserializeAlignment(a.getRisquesAlignmentJson()))
                .orElseGet(List::of);
    }

    /**
     * Helper public pour les consommateurs ({@code CaseFileDashboardService},
     * tests). Désérialise le JSON persisté ; renvoie liste vide en cas
     * d'échec (fail-open).
     */
    public List<RisqueAlignment> deserializeAlignment(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<RisqueAlignment> list = MAPPER.readValue(json, LIST_OF_ALIGNMENT);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("F-195: failed to deserialize risques_alignment_json — empty list");
            return List.of();
        }
    }
}
