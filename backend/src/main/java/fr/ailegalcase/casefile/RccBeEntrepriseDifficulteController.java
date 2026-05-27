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
 * SF-219-03 : endpoints REST de l'outil d'analyse de l'éligibilité au RCC
 * BE <b>entreprise en difficulté / restructuration</b> (Loi 26/12/2013 +
 * CCT n° 17 du 19/12/1974 + AR du 03/05/2007 + AR de reconnaissance
 * ministérielle — conditions âge réduit du plan / carrière ≥ 10 ans /
 * ancienneté secteur ≥ 5 ans + reconnaissance ministre obligatoire).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (aucun équivalent strict en droit français du RCC entreprise
 * en difficulté).</p>
 */
@RestController
@RequestMapping(
        "/api/v1/case-files/{caseFileId}/decision-tools/rcc-be-entreprise-difficulte")
public class RccBeEntrepriseDifficulteController {

    private final RccBeEntrepriseDifficulteService service;

    public RccBeEntrepriseDifficulteController(
            RccBeEntrepriseDifficulteService service) {
        this.service = service;
    }

    @PostMapping
    public RccBeEntrepriseDifficulteResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody RccBeEntrepriseDifficulteRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RccBeEntrepriseDifficulteResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
