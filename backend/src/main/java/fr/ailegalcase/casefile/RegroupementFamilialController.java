package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-03 : endpoints POST/GET pour l'analyse d'éligibilité au regroupement
 * familial L. 434-1+ CESEDA. Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/regroupement-familial-analysis")
public class RegroupementFamilialController {

    private final RegroupementFamilialService service;

    public RegroupementFamilialController(RegroupementFamilialService service) {
        this.service = service;
    }

    @PostMapping
    public RegroupementFamilialResponse analyze(@PathVariable UUID caseFileId,
                                                @RequestBody RegroupementFamilialRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RegroupementFamilialResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
