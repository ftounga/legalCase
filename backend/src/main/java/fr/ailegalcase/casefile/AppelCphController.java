package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-01 : endpoints POST/GET pour l'analyse de l'appel d'un jugement CPH
 * devant la chambre sociale de la Cour d'appel (art. 538 CPC ; R. 1461-1 CPC).
 * Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/appel-cph-analysis")
public class AppelCphController {

    private final AppelCphService service;

    public AppelCphController(AppelCphService service) {
        this.service = service;
    }

    @PostMapping
    public AppelCphResponse analyze(@PathVariable UUID caseFileId,
                                    @RequestBody AppelCphRequest request,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AppelCphResponse get(@PathVariable UUID caseFileId,
                                @AuthenticationPrincipal OidcUser oidcUser,
                                Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
