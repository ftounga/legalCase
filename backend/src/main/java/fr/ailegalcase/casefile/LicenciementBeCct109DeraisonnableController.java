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
 * SF-213-10 : endpoints REST de l'outil d'analyse du score CCT n° 109 BE —
 * licenciement manifestement déraisonnable (art. 9 CCT 12/02/2014).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (la France a un dispositif distinct — barème macron L. 1235-3
 * C. trav. ou licenciement sans cause réelle et sérieuse — non comparable).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-cct109-deraisonnable")
public class LicenciementBeCct109DeraisonnableController {

    private final LicenciementBeCct109DeraisonnableService service;

    public LicenciementBeCct109DeraisonnableController(
            LicenciementBeCct109DeraisonnableService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementBeCct109DeraisonnableResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody LicenciementBeCct109DeraisonnableRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementBeCct109DeraisonnableResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
