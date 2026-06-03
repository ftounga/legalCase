package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-37 : endpoints POST/GET pour l'analyse de monétisation de jours de RTT
 * (loi n° 2022-1157 du 16/08/2022 art. 5, F-DT-51). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/rtt-monetisation-analysis")
public class RttMonetisationController {

    private final RttMonetisationService service;

    public RttMonetisationController(RttMonetisationService service) {
        this.service = service;
    }

    @PostMapping
    public RttMonetisationResponse analyze(@PathVariable UUID caseFileId,
                                           @RequestBody RttMonetisationRequest request,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RttMonetisationResponse get(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
