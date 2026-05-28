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
 * SF-219-19 : endpoints REST de l'outil <i>droit à la déconnexion BE</i>
 * (Loi du 03/10/2022 « Deal pour l'emploi », art. 16 + AR 19/02/2023
 * + CCT n° 149).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. Le droit à la déconnexion français (C. trav. art.
 * L. 2242-17, 7° ; Loi n° 2016-1088 du 08/08/2016 dite « El Khomri »)
 * constitue un régime distinct (obligation de négociation annuelle
 * dans les entreprises ≥ 50 salariés et non obligation de résultat
 * pesant sur les entreprises ≥ 20 travailleurs). Restitution séparée
 * par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/droit-deconnexion-be")
public class DroitDeconnexionBeController {

    private final DroitDeconnexionBeService service;

    public DroitDeconnexionBeController(
            DroitDeconnexionBeService service) {
        this.service = service;
    }

    @PostMapping
    public DroitDeconnexionBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody DroitDeconnexionBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DroitDeconnexionBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
