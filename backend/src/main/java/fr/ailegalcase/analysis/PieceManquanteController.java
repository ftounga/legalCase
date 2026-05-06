package fr.ailegalcase.analysis;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * F-194 SF-194-01 — Contrôleur pièces manquantes markables.
 *
 * <ul>
 *   <li>{@code PUT /api/v1/case-files/{id}/pieces-manquantes/status} — upsert
 *       statut avocat trichotomique sur une pièce (par libellé original
 *       normalisé). Acte pur, aucun side-effect : tous les effets se
 *       déclenchent au prochain run de Synthèse enrichie via
 *       {@link PieceManquanteAlignmentService#materializeForAnalysis}.</li>
 *   <li>{@code GET /api/v1/case-files/{id}/pieces-manquantes-alignment} —
 *       lecture pure de l'alignement matérialisé pour la dernière analyse
 *       {@code DONE} du dossier (depuis
 *       {@code case_analyses.pieces_alignment_json}).</li>
 * </ul>
 *
 * <p>Pattern miroir {@link RetainedPisteAlignmentController} (F-192) +
 * {@link ProcedureCheckController} (F-96/F-193).</p>
 */
@RestController
public class PieceManquanteController {

    private final PieceManquanteStatusService pieceManquanteStatusService;
    private final PieceManquanteAlignmentService pieceManquanteAlignmentService;

    public PieceManquanteController(PieceManquanteStatusService pieceManquanteStatusService,
                                     PieceManquanteAlignmentService pieceManquanteAlignmentService) {
        this.pieceManquanteStatusService = pieceManquanteStatusService;
        this.pieceManquanteAlignmentService = pieceManquanteAlignmentService;
    }

    /**
     * Upsert idempotent du statut. Body :
     * <pre>{@code
     * {
     *   "pieceLibelleOriginal": "Contrat de travail original",
     *   "statut": "A_DEMANDER" | "OBTENUE" | "NON_APPLICABLE",
     *   "raisonNonApp": "...",   // requis si statut = NON_APPLICABLE, sinon null/absent
     *   "destinataire": "client" // optionnel
     * }
     * }</pre>
     */
    @PutMapping("/api/v1/case-files/{caseFileId}/pieces-manquantes/status")
    public PieceManquanteStatusResponse upsertStatus(@PathVariable UUID caseFileId,
                                                      @RequestBody Map<String, String> body,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body manquant");
        }
        String libelle = body.get("pieceLibelleOriginal");
        String statut = body.get("statut");
        String raisonNonApp = body.get("raisonNonApp");
        String destinataire = body.get("destinataire");

        PieceManquanteStatus saved = pieceManquanteStatusService.upsertStatus(
                caseFileId, libelle, statut, raisonNonApp, destinataire, oidcUser, principal);
        return PieceManquanteStatusResponse.from(saved);
    }

    /**
     * Lecture pure. Renvoie {@code []} si aucune analyse {@code DONE} ou si
     * l'alignement est vide / legacy (pré-F-194).
     */
    @GetMapping("/api/v1/case-files/{caseFileId}/pieces-manquantes-alignment")
    public List<PieceManquanteAlignment> getAlignment(@PathVariable UUID caseFileId,
                                                       @AuthenticationPrincipal OidcUser oidcUser,
                                                       Principal principal) {
        return pieceManquanteAlignmentService.getForLatestAnalysis(caseFileId, oidcUser, principal);
    }

    /** DTO réponse pour le PUT — projection minimale de {@link PieceManquanteStatus}. */
    public record PieceManquanteStatusResponse(
            UUID id,
            UUID caseFileId,
            String pieceLibelleOriginal,
            String pieceLibelleNormalise,
            String statut,
            String raisonNonApp,
            String destinataire) {

        public static PieceManquanteStatusResponse from(PieceManquanteStatus s) {
            return new PieceManquanteStatusResponse(
                    s.getId(),
                    s.getCaseFile() != null ? s.getCaseFile().getId() : null,
                    s.getPieceLibelleOriginal(),
                    s.getPieceLibelleNormalise(),
                    s.getStatut(),
                    s.getRaisonNonApp(),
                    s.getDestinataire());
        }
    }
}
