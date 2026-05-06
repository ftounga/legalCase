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
 * F-194 SF-194-01 — CRUD upsert (PUT pur, sans side-effect) du statut avocat
 * trichotomique sur les pièces manquantes IA, et lecture
 * {@link #collectForEnrichment(UUID)} consommée par
 * {@link EnrichedAnalysisService#buildEnrichedPrompt} pour injecter les sections
 * {@code [Pièces déjà obtenues]} / {@code [Pièces non applicables]} /
 * {@code [Pièces à demander]} dans le prompt enrichi.
 *
 * <p>Cohérence F-92 STRICTE : aucune mutation du JSON
 * {@code analysis_result.pieces_manquantes}. L'overlay statut vit dans la table
 * {@code piece_manquante_status} et n'est joint que par
 * {@link PieceManquanteAlignmentService#materializeForAnalysis} au run de
 * Synthèse enrichie.</p>
 *
 * <p>Pattern miroir {@link StrategicOptionService} (F-176) — le PUT statut
 * reste un acte pur, tous les effets matérialisés au prochain run de Synthèse
 * enrichie.</p>
 */
@Service
public class PieceManquanteStatusService {

    private static final Logger log = LoggerFactory.getLogger(PieceManquanteStatusService.class);

    /** Statuts acceptés en entrée du PUT. */
    private static final Set<String> VALID_STATUTS = Set.of(
            PieceManquanteStatus.STATUT_A_DEMANDER,
            PieceManquanteStatus.STATUT_OBTENUE,
            PieceManquanteStatus.STATUT_NON_APPLICABLE);

    private final PieceManquanteStatusRepository pieceManquanteStatusRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public PieceManquanteStatusService(PieceManquanteStatusRepository pieceManquanteStatusRepository,
                                        CaseFileRepository caseFileRepository,
                                        WorkspaceMemberRepository workspaceMemberRepository,
                                        CurrentUserResolver currentUserResolver) {
        this.pieceManquanteStatusRepository = pieceManquanteStatusRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Upsert idempotent du statut sur la clé
     * {@code (case_file_id, piece_libelle_normalise)} — strictement pas
     * d'effets de bord. Aucune modification de
     * {@code case_analyses.analysis_result.pieces_manquantes} (cohérence F-92
     * stricte garantie par test régression).
     *
     * @param caseFileId           id dossier (cible de l'isolation workspace)
     * @param pieceLibelleOriginal libellé original (1..500 chars, non blank)
     * @param statut               {@code A_DEMANDER} / {@code OBTENUE} / {@code NON_APPLICABLE}
     * @param raisonNonApp         raison libre — ne doit être fournie que si {@code statut = NON_APPLICABLE}
     * @param destinataire         destinataire libre (≤ 200 chars, optionnel)
     * @return l'entrée upsertée
     */
    @Transactional
    public PieceManquanteStatus upsertStatus(UUID caseFileId,
                                              String pieceLibelleOriginal,
                                              String statut,
                                              String raisonNonApp,
                                              String destinataire,
                                              OidcUser oidcUser,
                                              Principal principal) {
        // Validation entrée
        if (pieceLibelleOriginal == null || pieceLibelleOriginal.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Champ 'pieceLibelleOriginal' requis");
        }
        if (pieceLibelleOriginal.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'pieceLibelleOriginal' trop long (max 500 caractères)");
        }
        if (statut == null || !VALID_STATUTS.contains(statut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Statut invalide (attendu : A_DEMANDER / OBTENUE / NON_APPLICABLE)");
        }
        if (raisonNonApp != null && !raisonNonApp.trim().isEmpty()
                && !PieceManquanteStatus.STATUT_NON_APPLICABLE.equals(statut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'raisonNonApp' n'est autorisé que pour statut NON_APPLICABLE");
        }
        if (destinataire != null && destinataire.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'destinataire' trop long (max 200 caractères)");
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

        String normalized = normalize(pieceLibelleOriginal);

        // Upsert idempotent sur clé (case_file_id, piece_libelle_normalise)
        PieceManquanteStatus entity = pieceManquanteStatusRepository
                .findByCaseFileIdAndPieceLibelleNormalise(caseFileId, normalized)
                .orElseGet(() -> {
                    PieceManquanteStatus e = new PieceManquanteStatus();
                    e.setCaseFile(caseFile);
                    e.setWorkspace(workspace);
                    e.setPieceLibelleNormalise(normalized);
                    return e;
                });

        // Toujours rafraîchir le libellé original (capture la dernière forme vue par l'avocat)
        entity.setPieceLibelleOriginal(pieceLibelleOriginal.trim());
        entity.setStatut(statut);
        entity.setRaisonNonApp(
                PieceManquanteStatus.STATUT_NON_APPLICABLE.equals(statut)
                        ? (raisonNonApp != null && !raisonNonApp.trim().isEmpty() ? raisonNonApp.trim() : null)
                        : null);
        entity.setDestinataire(destinataire != null && !destinataire.trim().isEmpty()
                ? destinataire.trim() : null);

        return pieceManquanteStatusRepository.save(entity);
    }

    /**
     * Snapshot des statuts pièces avocat pour injection dans le prompt enrichi.
     * Fail-open : retourne {@link EnrichmentSnapshot#empty()} en cas d'erreur.
     */
    @Transactional(readOnly = true)
    public EnrichmentSnapshot collectForEnrichment(UUID caseFileId) {
        if (caseFileId == null) return EnrichmentSnapshot.empty();
        try {
            List<PieceManquanteStatus> all = pieceManquanteStatusRepository.findByCaseFileId(caseFileId);
            List<String> obtenues = new ArrayList<>();
            List<String> nonApplicables = new ArrayList<>();
            List<String> aDemander = new ArrayList<>();
            for (PieceManquanteStatus s : all) {
                String libelle = s.getPieceLibelleOriginal();
                if (libelle == null || libelle.isBlank()) continue;
                switch (s.getStatut()) {
                    case PieceManquanteStatus.STATUT_OBTENUE -> obtenues.add(libelle);
                    case PieceManquanteStatus.STATUT_NON_APPLICABLE -> nonApplicables.add(libelle);
                    case PieceManquanteStatus.STATUT_A_DEMANDER -> aDemander.add(libelle);
                    default -> { /* ignore */ }
                }
            }
            return new EnrichmentSnapshot(obtenues, nonApplicables, aDemander);
        } catch (Exception e) {
            log.warn("F-194: collectForEnrichment failed for caseFileId {} — empty snapshot", caseFileId, e);
            return EnrichmentSnapshot.empty();
        }
    }

    /**
     * Snapshot trichotomique pour le prompt enrichi.
     */
    public record EnrichmentSnapshot(List<String> obtenues,
                                      List<String> nonApplicables,
                                      List<String> aDemander) {
        public static EnrichmentSnapshot empty() {
            return new EnrichmentSnapshot(List.of(), List.of(), List.of());
        }
    }

    /** Normalisation du libellé : trim + lowercase. */
    static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }
}
