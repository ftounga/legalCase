package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-39 : endpoints POST/GET pour l'analyse du droit au séjour UE/EEE/Suisse
 * en France (directive 2004/38, L. 233-1+ CESEDA). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/ue-eee-suisse-sejour-analysis")
public class UeEeeSuisseSejourController {

    private final UeEeeSuisseSejourService service;

    public UeEeeSuisseSejourController(UeEeeSuisseSejourService service) {
        this.service = service;
    }

    @PostMapping
    public UeEeeSuisseSejourResponse analyze(@PathVariable UUID caseFileId,
                                             @RequestBody UeEeeSuisseSejourRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public UeEeeSuisseSejourResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
