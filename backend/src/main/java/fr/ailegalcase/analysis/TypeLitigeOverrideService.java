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
import java.util.Set;
import java.util.UUID;

/**
 * F-197 SF-197-01 — CRUD upsert (PUT pur) du override avocat sur le
 * {@code type_litige_detecte} (Travail FR) ou {@code type_procedure_detectee}
 * (Immigration FR/BE), et lecture {@link #getForCaseFile} consommée par
 * l'endpoint GET.
 *
 * <p>Cohérence F-176 stricte : aucune mutation du JSON {@code analysis_result},
 * aucune side-effect ailleurs. Tous les effets matérialisés au prochain run de
 * Synthèse enrichie ({@link EnrichedAnalysisService}) :
 * <ol>
 *   <li>injection dans le prompt enrichi (section {@code [Type litige fixé par l'avocat]}) ;</li>
 *   <li>clonage automatique sur la nouvelle analyse créée ;</li>
 *   <li>lecture en priorité sur l'IA dans {@code DecisionToolVisibilityService}
 *       pour la résolution F-IA-04.</li>
 * </ol>
 *
 * <p>Pattern de référence : {@link RisqueStatusService} (F-195) pour
 * l'isolation workspace + le PUT pur. Adaptation pour single-value override
 * (pas de table dédiée — 3 colonnes nullable directement sur {@code case_analyses}).</p>
 */
@Service
public class TypeLitigeOverrideService {

    private static final Logger log = LoggerFactory.getLogger(TypeLitigeOverrideService.class);

    /** Travail FR — 7 valeurs autorisées (cf. mini-spec, miroir LitigationTypeMapper). */
    static final Set<String> TRAVAIL_VALID_TYPES = Set.of(
            "LICENCIEMENT_SANS_CAUSE_REELLE",
            "LICENCIEMENT_ECONOMIQUE",
            "PRISE_ACTE_RUPTURE",
            "HARCELEMENT_MORAL",
            "DISCRIMINATION",
            "HEURES_SUPPLEMENTAIRES",
            "RAPPEL_SALAIRE");

    /** Immigration FR/BE — codes alignés sur LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION. */
    static final Set<String> IMMIGRATION_VALID_TYPES = Set.of(
            "RENOUVELLEMENT_TITRE_SEJOUR",
            "DEMANDE_ASILE_OFPRA",
            "RECOURS_CNDA",
            "REGROUPEMENT_FAMILIAL",
            "NATURALISATION_DECRET",
            "CHANGEMENT_STATUT",
            "AES_SALARIE",
            "REGULARISATION_EXCEPTIONNELLE",
            "OQTF_AVEC_DELAI",
            "OQTF_SANS_DELAI");

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public TypeLitigeOverrideService(CaseAnalysisRepository caseAnalysisRepository,
                                      CaseFileRepository caseFileRepository,
                                      WorkspaceMemberRepository workspaceMemberRepository,
                                      CurrentUserResolver currentUserResolver) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Upsert l'override sur la dernière analyse {@code DONE} du dossier.
     *
     * <p>Validation :
     * <ul>
     *   <li>{@code type} non null/blank → 400</li>
     *   <li>{@code type} non reconnu pour le domaine du dossier → 400 "Type non applicable au domaine"</li>
     *   <li>aucune analyse {@code DONE} → 404 "Aucune analyse à overrider"</li>
     *   <li>dossier d'un autre workspace → 404 camouflage</li>
     * </ul>
     *
     * @return l'analyse modifiée
     */
    @Transactional
    public CaseAnalysis upsertOverride(UUID caseFileId,
                                        String type,
                                        String raison,
                                        OidcUser oidcUser,
                                        Principal principal) {
        if (type == null || type.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champ 'type' requis");
        }
        String normalized = type.trim().toUpperCase();

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

        // Validation type vs domaine du dossier
        String legalDomain = caseFile.getLegalDomain();
        boolean isTravail = TRAVAIL_VALID_TYPES.contains(normalized);
        boolean isImmigration = IMMIGRATION_VALID_TYPES.contains(normalized);
        if (!isTravail && !isImmigration) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type non valide (attendu : Travail FR ou Immigration)");
        }
        if (isTravail && !"DROIT_DU_TRAVAIL".equals(legalDomain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type non applicable au domaine");
        }
        if (isImmigration && !"DROIT_IMMIGRATION".equals(legalDomain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type non applicable au domaine");
        }

        // Lecture de la dernière analyse DONE
        CaseAnalysis latest = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse à overrider"));

        // Upsert : remplace simplement la valeur précédente (pas d'historique V1)
        if (isTravail) {
            latest.setTypeLitigeAvocatOverride(normalized);
            latest.setTypeProcedureAvocatOverride(null);
        } else {
            latest.setTypeProcedureAvocatOverride(normalized);
            latest.setTypeLitigeAvocatOverride(null);
        }
        if (raison != null && !raison.trim().isEmpty()) {
            latest.setTypeOverrideRaison(raison.trim());
        } else {
            latest.setTypeOverrideRaison(null);
        }
        return caseAnalysisRepository.save(latest);
    }

    /**
     * Lecture pure du override courant pour le dossier — retourne l'override de
     * la dernière analyse {@code DONE}, ou {@code TypeLitigeOverrideResponse(null, null, null)}
     * si aucune analyse ou aucun override.
     */
    @Transactional(readOnly = true)
    public TypeLitigeOverrideResponse getForCaseFile(UUID caseFileId,
                                                      OidcUser oidcUser,
                                                      Principal principal) {
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

        return caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .map(TypeLitigeOverrideResponse::from)
                .orElseGet(() -> new TypeLitigeOverrideResponse(null, null, null));
    }

    /**
     * F-197 SF-197-01 — Clone l'override depuis l'analyse précédente vers la nouvelle.
     * Appelé par {@link EnrichedAnalysisService} après création de la nouvelle analyse
     * pour propager automatiquement le choix de l'avocat (évite à l'avocat de re-saisir
     * à chaque run).
     *
     * <p>Fail-open : si la lecture/écriture échoue, le run continue avec la nouvelle
     * analyse sans override (log warn).</p>
     */
    @Transactional
    public void cloneOverrideFromPrevious(UUID previousAnalysisId, CaseAnalysis newAnalysis) {
        if (previousAnalysisId == null || newAnalysis == null) return;
        try {
            CaseAnalysis previous = caseAnalysisRepository.findById(previousAnalysisId).orElse(null);
            if (previous == null) return;
            String typeLitige = previous.getTypeLitigeAvocatOverride();
            String typeProcedure = previous.getTypeProcedureAvocatOverride();
            String raison = previous.getTypeOverrideRaison();
            if (typeLitige == null && typeProcedure == null) {
                // rien à cloner
                return;
            }
            newAnalysis.setTypeLitigeAvocatOverride(typeLitige);
            newAnalysis.setTypeProcedureAvocatOverride(typeProcedure);
            newAnalysis.setTypeOverrideRaison(raison);
            caseAnalysisRepository.save(newAnalysis);
            log.debug("F-197: cloned override from analysis {} to {} (typeLitige={}, typeProcedure={})",
                    previousAnalysisId, newAnalysis.getId(), typeLitige, typeProcedure);
        } catch (Exception e) {
            log.warn("F-197: cloneOverrideFromPrevious fail-open for analysis {} (previous {})",
                    newAnalysis.getId(), previousAnalysisId, e);
        }
    }

    /**
     * F-197 SF-197-01 — Helper pour récupérer l'override courant d'une analyse
     * (lecture sans isolation workspace — usage interne backend uniquement).
     * Retourne {@code null} si aucune analyse ou aucun override actif.
     */
    @Transactional(readOnly = true)
    public OverrideSnapshot readOverrideForLatestDone(UUID caseFileId) {
        return caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .map(a -> {
                    String t = a.getTypeLitigeAvocatOverride();
                    String p = a.getTypeProcedureAvocatOverride();
                    if (t == null && p == null) return null;
                    return new OverrideSnapshot(t, p, a.getTypeOverrideRaison());
                })
                .orElse(null);
    }

    /** Snapshot léger pour usage interne (prompt enrichi, visibility). */
    public record OverrideSnapshot(String typeLitige, String typeProcedure, String raison) {
    }
}
