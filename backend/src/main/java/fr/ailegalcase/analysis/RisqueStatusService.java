package fr.ailegalcase.analysis;

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
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * F-195 SF-195-01 — CRUD upsert (PUT pur, sans side-effect) du statut avocat
 * trichotomique sur les risques IA, et lecture {@link #collectForEnrichment(UUID)}
 * consommée par {@link EnrichedAnalysisService#buildEnrichedPrompt} pour
 * injecter les sections {@code [Risques validés par votre avocat — à approfondir]}
 * et {@code [Risques écartés — NE PAS re-proposer]} dans le prompt enrichi.
 *
 * <p>Cohérence F-IA-02 STRICTE : aucune mutation du JSON
 * {@code analysis_result.risques} ni du {@code score_risque} IA brut. L'overlay
 * statut vit dans la table {@code risque_status} et n'est joint que par
 * {@link RisqueAlignmentService#materializeForAnalysis} au run de Synthèse
 * enrichie. Le score recomputé excluant les ÉCARTÉ est stocké séparément dans
 * {@code case_analyses.score_risque_avocat_json} — l'avocat voit les 2 (transparence).</p>
 *
 * <p>Pattern miroir {@link PieceManquanteStatusService} (F-194) — le PUT statut
 * reste un acte pur, tous les effets matérialisés au prochain run de Synthèse
 * enrichie.</p>
 */
@Service
public class RisqueStatusService {

    private static final Logger log = LoggerFactory.getLogger(RisqueStatusService.class);

    /** Statuts acceptés en entrée du PUT. */
    private static final Set<String> VALID_STATUTS = Set.of(
            RisqueStatus.STATUT_A_CREUSER,
            RisqueStatus.STATUT_VALIDE,
            RisqueStatus.STATUT_ECARTE);

    private final RisqueStatusRepository risqueStatusRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public RisqueStatusService(RisqueStatusRepository risqueStatusRepository,
                                CaseFileRepository caseFileRepository,
                                WorkspaceMemberRepository workspaceMemberRepository,
                                CurrentUserResolver currentUserResolver) {
        this.risqueStatusRepository = risqueStatusRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Upsert idempotent du statut sur la clé
     * {@code (case_file_id, risque_libelle_normalise)} — strictement pas
     * d'effets de bord. Aucune modification de
     * {@code case_analyses.analysis_result.risques} ni du {@code score_risque}
     * IA brut (cohérence F-IA-02 stricte garantie par test régression).
     *
     * @param caseFileId            id dossier (cible de l'isolation workspace)
     * @param risqueLibelleOriginal libellé original (1..500 chars, non blank)
     * @param statut                {@code A_CREUSER} / {@code VALIDE} / {@code ECARTE}
     * @param raisonEcarte          raison libre — ne doit être fournie que si {@code statut = ECARTE}
     * @return l'entrée upsertée
     */
    @Transactional
    public RisqueStatus upsertStatus(UUID caseFileId,
                                      String risqueLibelleOriginal,
                                      String statut,
                                      String raisonEcarte,
                                      OidcUser oidcUser,
                                      Principal principal) {
        // Validation entrée
        if (risqueLibelleOriginal == null || risqueLibelleOriginal.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Champ 'risqueLibelleOriginal' requis");
        }
        if (risqueLibelleOriginal.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'risqueLibelleOriginal' trop long (max 500 caractères)");
        }
        if (statut == null || !VALID_STATUTS.contains(statut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Statut invalide (attendu : A_CREUSER / VALIDE / ECARTE)");
        }
        if (raisonEcarte != null && !raisonEcarte.trim().isEmpty()
                && !RisqueStatus.STATUT_ECARTE.equals(statut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'raisonEcarte' n'est autorisé que pour statut ECARTE");
        }

        // Résolution workspace + isolation 404 camouflage
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        Workspace workspace = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        String normalized = normalize(risqueLibelleOriginal);

        // Upsert idempotent sur clé (case_file_id, risque_libelle_normalise)
        RisqueStatus entity = risqueStatusRepository
                .findByCaseFileIdAndRisqueLibelleNormalise(caseFileId, normalized)
                .orElseGet(() -> {
                    RisqueStatus e = new RisqueStatus();
                    e.setCaseFile(caseFile);
                    e.setWorkspace(workspace);
                    e.setRisqueLibelleNormalise(normalized);
                    return e;
                });

        // Toujours rafraîchir le libellé original (capture la dernière forme vue par l'avocat)
        entity.setRisqueLibelleOriginal(risqueLibelleOriginal.trim());
        entity.setStatut(statut);
        entity.setRaisonEcarte(
                RisqueStatus.STATUT_ECARTE.equals(statut)
                        ? (raisonEcarte != null && !raisonEcarte.trim().isEmpty() ? raisonEcarte.trim() : null)
                        : null);

        return risqueStatusRepository.save(entity);
    }

    /**
     * Snapshot des statuts risques avocat pour injection dans le prompt enrichi.
     * Fail-open : retourne {@link EnrichmentSnapshot#empty()} en cas d'erreur.
     */
    @Transactional(readOnly = true)
    public EnrichmentSnapshot collectForEnrichment(UUID caseFileId) {
        if (caseFileId == null) return EnrichmentSnapshot.empty();
        try {
            List<RisqueStatus> all = risqueStatusRepository.findByCaseFileId(caseFileId);
            List<String> valides = new ArrayList<>();
            List<EcarteEntry> ecartes = new ArrayList<>();
            List<String> aCreuser = new ArrayList<>();
            for (RisqueStatus s : all) {
                String libelle = s.getRisqueLibelleOriginal();
                if (libelle == null || libelle.isBlank()) continue;
                switch (s.getStatut()) {
                    case RisqueStatus.STATUT_VALIDE -> valides.add(libelle);
                    case RisqueStatus.STATUT_ECARTE -> ecartes.add(new EcarteEntry(libelle, s.getRaisonEcarte()));
                    case RisqueStatus.STATUT_A_CREUSER -> aCreuser.add(libelle);
                    default -> { /* ignore */ }
                }
            }
            return new EnrichmentSnapshot(valides, ecartes, aCreuser);
        } catch (Exception e) {
            log.warn("F-195: collectForEnrichment failed for caseFileId {} — empty snapshot", caseFileId, e);
            return EnrichmentSnapshot.empty();
        }
    }

    /**
     * Snapshot trichotomique pour le prompt enrichi.
     * Les écartés conservent leur raison pour permettre à l'IA de comprendre le contexte.
     */
    public record EnrichmentSnapshot(List<String> valides,
                                      List<EcarteEntry> ecartes,
                                      List<String> aCreuser) {
        public static EnrichmentSnapshot empty() {
            return new EnrichmentSnapshot(List.of(), List.of(), List.of());
        }
    }

    /** Entrée pour un risque écarté avec raison facultative. */
    public record EcarteEntry(String libelle, String raison) {}

    /** Normalisation du libellé : trim + lowercase. */
    static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }
}
