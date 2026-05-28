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
 * SF-219-10 : endpoints REST de l'outil <i>statut du délégué syndical</i>
 * (CCT n° 5 du 24/05/1971 — statut des délégations syndicales du
 * personnel des entreprises BE).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (la France a un régime de délégué syndical sous C. trav.
 * art. L. 2143-1 et s. mais avec un régime de procédure radicalement
 * distinct : élection au CSE vs désignation par OS représentative). Le
 * volet protection contre le licenciement est traité par un outil
 * distinct (SF-213-08 <i>licenciement-be-protection-deleguee</i>).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/delegue-syndical-cct-5")
public class DelegueSyndicalCct5Controller {

    private final DelegueSyndicalCct5Service service;

    public DelegueSyndicalCct5Controller(DelegueSyndicalCct5Service service) {
        this.service = service;
    }

    @PostMapping
    public DelegueSyndicalCct5Response analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody DelegueSyndicalCct5Request request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DelegueSyndicalCct5Response get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
