package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-53 : endpoints POST/GET pour l'analyse de conformité à l'obligation
 * relative au droit à la déconnexion (art. L.2242-17 7° CT, F-DT-83). Outil
 * single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/droit-deconnexion-conformite-analysis")
public class DroitDeconnexionConformiteController {

    private final DroitDeconnexionConformiteService service;

    public DroitDeconnexionConformiteController(DroitDeconnexionConformiteService service) {
        this.service = service;
    }

    @PostMapping
    public DroitDeconnexionConformiteResponse analyze(@PathVariable UUID caseFileId,
                                                      @RequestBody DroitDeconnexionConformiteRequest request,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DroitDeconnexionConformiteResponse get(@PathVariable UUID caseFileId,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
