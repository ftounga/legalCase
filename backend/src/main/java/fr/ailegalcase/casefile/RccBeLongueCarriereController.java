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
 * SF-219-02 : endpoints REST de l'outil d'analyse de l'éligibilité au RCC
 * BE <b>longue carrière</b> (Loi 26/12/2013 + CCT n° 17 du 19/12/1974 +
 * AR du 03/05/2007 art. 3 — conditions 59 ans / 40 ans de carrière).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (aucun équivalent strict en droit français du RCC longue
 * carrière).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/rcc-be-longue-carriere")
public class RccBeLongueCarriereController {

    private final RccBeLongueCarriereService service;

    public RccBeLongueCarriereController(RccBeLongueCarriereService service) {
        this.service = service;
    }

    @PostMapping
    public RccBeLongueCarriereResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody RccBeLongueCarriereRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RccBeLongueCarriereResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
