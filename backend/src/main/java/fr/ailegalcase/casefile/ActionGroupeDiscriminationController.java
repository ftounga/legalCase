package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-09 : endpoints POST/GET pour l'analyse de recevabilité d'une action de
 * groupe en discrimination au travail (art. L. 1134-7 à L. 1134-10 Code
 * travail). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/action-groupe-discrimination-analysis")
public class ActionGroupeDiscriminationController {

    private final ActionGroupeDiscriminationService service;

    public ActionGroupeDiscriminationController(ActionGroupeDiscriminationService service) {
        this.service = service;
    }

    @PostMapping
    public ActionGroupeDiscriminationResponse analyze(@PathVariable UUID caseFileId,
                                                      @RequestBody ActionGroupeDiscriminationRequest request,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ActionGroupeDiscriminationResponse get(@PathVariable UUID caseFileId,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
