package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-26-01 : endpoint REST changement d'état civil — nom, prénom, sexe
 * (art. 60 / 61-3-1 / 61-5 Cciv).
 * Single-country FR DROIT_FAMILLE.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/changement-etat-civil")
public class ChangementEtatCivilController {

    private final ChangementEtatCivilService service;

    public ChangementEtatCivilController(ChangementEtatCivilService service) {
        this.service = service;
    }

    @PostMapping
    public ChangementEtatCivilResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody ChangementEtatCivilRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ChangementEtatCivilResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
