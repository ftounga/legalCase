package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-33 : endpoints POST/GET pour l'analyse du statut et de la protection
 * d'un délégué syndical (DS) ou représentant de section syndicale (RSS)
 * (art. L.2143-1 et s., L.2142-1-1, L.2143-3, L.2411-3 CT, F-DT-69). Outil
 * single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/delegation-syndicale-analysis")
public class DelegationSyndicaleController {

    private final DelegationSyndicaleService service;

    public DelegationSyndicaleController(DelegationSyndicaleService service) {
        this.service = service;
    }

    @PostMapping
    public DelegationSyndicaleResponse analyze(@PathVariable UUID caseFileId,
                                               @RequestBody DelegationSyndicaleRequest request,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DelegationSyndicaleResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
