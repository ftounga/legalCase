package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-07 : endpoints POST/GET pour l'analyse de la saisie sur rémunération
 * (quotité saisissable — art. R. 3252-2 et s. Code du travail). Outil
 * single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/saisie-remuneration-analysis")
public class SaisieRemunerationController {

    private final SaisieRemunerationService service;

    public SaisieRemunerationController(SaisieRemunerationService service) {
        this.service = service;
    }

    @PostMapping
    public SaisieRemunerationResponse analyze(@PathVariable UUID caseFileId,
                                              @RequestBody SaisieRemunerationRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public SaisieRemunerationResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
