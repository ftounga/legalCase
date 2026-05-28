package fr.ailegalcase.casefile;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-219-25 : endpoints REST de l'outil <i>Auditorat du travail BE —
 * orientation et checklist de saisine du parquet spécialisé en droit
 * social pénal</i> (Code judiciaire art. 138bis + Code d'instruction
 * criminelle art. 24 + Loi du 03/08/1992 sur le Code judiciaire).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. La France n'a pas d'auditorat du travail : les infractions
 * au droit du travail sont poursuivies par le procureur de la
 * République sur PV de l'inspection du travail (Code du travail
 * art. L. 8112-1 et s.). Pas d'équivalent direct, restitution
 * séparée par outil — pas de tentative d'harmonisation FR/BE.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/auditorat-travail-be")
public class AuditoratTravailBeController {

    private final AuditoratTravailBeService service;

    public AuditoratTravailBeController(
            AuditoratTravailBeService service) {
        this.service = service;
    }

    @PostMapping
    public AuditoratTravailBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody AuditoratTravailBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AuditoratTravailBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
