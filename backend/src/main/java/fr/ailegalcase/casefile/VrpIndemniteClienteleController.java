package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-11 : endpoints POST/GET pour l'analyse de la rupture d'un VRP statutaire
 * (statut, préavis, indemnité de clientèle — art. L.7311-1 et s. CT). Outil
 * single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/vrp-indemnite-clientele-analysis")
public class VrpIndemniteClienteleController {

    private final VrpIndemniteClienteleService service;

    public VrpIndemniteClienteleController(VrpIndemniteClienteleService service) {
        this.service = service;
    }

    @PostMapping
    public VrpIndemniteClienteleResponse analyze(@PathVariable UUID caseFileId,
                                                 @RequestBody VrpIndemniteClienteleRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public VrpIndemniteClienteleResponse get(@PathVariable UUID caseFileId,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
