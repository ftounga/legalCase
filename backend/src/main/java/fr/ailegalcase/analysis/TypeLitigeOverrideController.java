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
import java.util.UUID;

/**
 * F-197 SF-197-01 — Contrôleur override avocat du {@code type_litige_detecte} (Travail FR)
 * et {@code type_procedure_detectee} (Immigration FR/BE).
 *
 * <ul>
 *   <li>{@code PUT /api/v1/case-files/{id}/type-litige-override} — upsert override
 *       sur la dernière analyse {@code DONE}. Acte pur, aucun side-effect : tous
 *       les effets se déclenchent au prochain run de Synthèse enrichie via
 *       {@link EnrichedAnalysisService} (clone sur la nouvelle analyse + section
 *       prompt enrichi + visibility F-IA-04 prioritaire sur l'IA).</li>
 *   <li>{@code GET /api/v1/case-files/{id}/type-litige-override} — lecture pure
 *       de l'override courant pour la dernière analyse {@code DONE}. Retourne
 *       {@code {typeLitigeAvocat:null, typeProcedureAvocat:null, raison:null}}
 *       si aucun override actif.</li>
 * </ul>
 *
 * <p>Pattern miroir {@link RisqueController} (F-195). Single-value override
 * (pas trichotomie) — différent des F-192/193/194/195 qui utilisent des tables
 * dédiées.</p>
 */
@RestController
public class TypeLitigeOverrideController {

    private final TypeLitigeOverrideService overrideService;

    public TypeLitigeOverrideController(TypeLitigeOverrideService overrideService) {
        this.overrideService = overrideService;
    }

    /**
     * Upsert override. Body :
     * <pre>{@code
     * {
     *   "type": "LICENCIEMENT_ECONOMIQUE",   // Travail FR ou Immigration FR/BE
     *   "raison": "Analyse documents : ..."  // optionnel
     * }
     * }</pre>
     *
     * <p>Retourne {@link TypeLitigeOverrideResponse} après sauvegarde.</p>
     */
    @PutMapping("/api/v1/case-files/{caseFileId}/type-litige-override")
    public TypeLitigeOverrideResponse upsertOverride(@PathVariable UUID caseFileId,
                                                      @RequestBody TypeLitigeOverrideRequest body,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body manquant");
        }
        CaseAnalysis saved = overrideService.upsertOverride(
                caseFileId, body.type(), body.raison(), oidcUser, principal);
        return TypeLitigeOverrideResponse.from(saved);
    }

    /**
     * Lecture pure. Renvoie l'override courant pour la dernière analyse DONE
     * du dossier, ou {@code (null, null, null)} si aucun override actif.
     */
    @GetMapping("/api/v1/case-files/{caseFileId}/type-litige-override")
    public TypeLitigeOverrideResponse getOverride(@PathVariable UUID caseFileId,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   Principal principal) {
        return overrideService.getForCaseFile(caseFileId, oidcUser, principal);
    }
}
