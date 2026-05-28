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
 * SF-219-12 : endpoints REST de l'outil <i>flexi-job BE</i>
 * (Loi-programme 26/12/2013 + extensions 2015/2018/2023).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. La France a un régime de « CDD d'usage » et de « CDI
 * intérimaire » dans des secteurs limitativement énumérés (C. trav.
 * art. L. 1242-2 § 3, art. L. 1251-58-1) qui partagent une logique
 * d'occupation atypique mais avec un régime de cotisations, de
 * formalisme (Dimona inexistante en FR) et de plafonds incomparables.
 * Restitution séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/flexi-job-be")
public class FlexiJobBeController {

    private final FlexiJobBeService service;

    public FlexiJobBeController(FlexiJobBeService service) {
        this.service = service;
    }

    @PostMapping
    public FlexiJobBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody FlexiJobBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public FlexiJobBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
