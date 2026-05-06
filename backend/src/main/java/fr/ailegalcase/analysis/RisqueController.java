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
 * F-195 SF-195-01 — Contrôleur risques markables.
 *
 * <ul>
 *   <li>{@code PUT /api/v1/case-files/{id}/risques/status} — upsert statut
 *       avocat trichotomique sur un risque (par libellé original normalisé).
 *       Acte pur, aucun side-effect : tous les effets se déclenchent au
 *       prochain run de Synthèse enrichie via
 *       {@link RisqueAlignmentService#materializeForAnalysis}.</li>
 *   <li>{@code GET /api/v1/case-files/{id}/risques-alignment} — lecture pure
 *       de l'alignement matérialisé pour la dernière analyse {@code DONE} du
 *       dossier (depuis {@code case_analyses.risques_alignment_json}).</li>
 * </ul>
 *
 * <p>Pattern miroir {@link PieceManquanteController} (F-194).</p>
 */
@RestController
public class RisqueController {

    private final RisqueStatusService risqueStatusService;
    private final RisqueAlignmentService risqueAlignmentService;

    public RisqueController(RisqueStatusService risqueStatusService,
                             RisqueAlignmentService risqueAlignmentService) {
        this.risqueStatusService = risqueStatusService;
        this.risqueAlignmentService = risqueAlignmentService;
    }

    /**
     * Upsert idempotent du statut. Body :
     * <pre>{@code
     * {
     *   "risqueLibelleOriginal": "Harcèlement moral subi",
     *   "statut": "A_CREUSER" | "VALIDE" | "ECARTE",
     *   "raisonEcarte": "..."   // requis si statut = ECARTE, sinon null/absent
     * }
     * }</pre>
     */
    @PutMapping("/api/v1/case-files/{caseFileId}/risques/status")
    public RisqueStatusResponse upsertStatus(@PathVariable UUID caseFileId,
                                              @RequestBody Map<String, String> body,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body manquant");
        }
        String libelle = body.get("risqueLibelleOriginal");
        String statut = body.get("statut");
        String raisonEcarte = body.get("raisonEcarte");

        RisqueStatus saved = risqueStatusService.upsertStatus(
                caseFileId, libelle, statut, raisonEcarte, oidcUser, principal);
        return RisqueStatusResponse.from(saved);
    }

    /**
     * Lecture pure. Renvoie {@code []} si aucune analyse {@code DONE} ou si
     * l'alignement est vide / legacy (pré-F-195).
     */
    @GetMapping("/api/v1/case-files/{caseFileId}/risques-alignment")
    public List<RisqueAlignment> getAlignment(@PathVariable UUID caseFileId,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return risqueAlignmentService.getForLatestAnalysis(caseFileId, oidcUser, principal);
    }

    /** DTO réponse pour le PUT — projection minimale de {@link RisqueStatus}. */
    public record RisqueStatusResponse(
            UUID id,
            UUID caseFileId,
            String risqueLibelleOriginal,
            String risqueLibelleNormalise,
            String statut,
            String raisonEcarte) {

        public static RisqueStatusResponse from(RisqueStatus s) {
            return new RisqueStatusResponse(
                    s.getId(),
                    s.getCaseFile() != null ? s.getCaseFile().getId() : null,
                    s.getRisqueLibelleOriginal(),
                    s.getRisqueLibelleNormalise(),
                    s.getStatut(),
                    s.getRaisonEcarte());
        }
    }
}
