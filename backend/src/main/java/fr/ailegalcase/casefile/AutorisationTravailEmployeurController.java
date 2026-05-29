package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-43 : endpoints POST/GET pour l'analyse des obligations de l'employeur
 * recrutant un travailleur étranger hors UE (autorisation de travail, L. 5221-1
 * Code du travail). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/autorisation-travail-employeur-analysis")
public class AutorisationTravailEmployeurController {

    private final AutorisationTravailEmployeurService service;

    public AutorisationTravailEmployeurController(AutorisationTravailEmployeurService service) {
        this.service = service;
    }

    @PostMapping
    public AutorisationTravailEmployeurResponse analyze(
            @PathVariable UUID caseFileId,
            @RequestBody AutorisationTravailEmployeurRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AutorisationTravailEmployeurResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
