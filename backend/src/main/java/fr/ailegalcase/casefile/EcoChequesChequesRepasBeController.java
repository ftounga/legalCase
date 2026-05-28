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
 * SF-219-21 : endpoints REST de l'outil <i>éco-chèques + chèques-repas
 * BE</i> (CCT n°98 du CNT + Loi 25/04/2014 + AR 03/02/2010 art. 19bis
 * AR 28/11/1969).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. Les titres-restaurant français (CGI art. 81 19° ter) reposent
 * sur un mécanisme analogue mais avec plafond d'exonération distinct
 * (7,18 EUR/jour en 2024, contribution employeur 50-60 %) — outil
 * séparé requis côté FR le cas échéant.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/eco-cheques-cheques-repas-be")
public class EcoChequesChequesRepasBeController {

    private final EcoChequesChequesRepasBeService service;

    public EcoChequesChequesRepasBeController(
            EcoChequesChequesRepasBeService service) {
        this.service = service;
    }

    @PostMapping
    public EcoChequesChequesRepasBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody EcoChequesChequesRepasBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public EcoChequesChequesRepasBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
