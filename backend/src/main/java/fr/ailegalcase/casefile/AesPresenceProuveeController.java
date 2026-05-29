package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-11 : endpoints POST/GET pour le calcul de présence prouvée en France et
 * l'éligibilité aux 4 voies AES (L. 435-1 / L. 435-3 CESEDA). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/aes-presence-prouvee-analysis")
public class AesPresenceProuveeController {

    private final AesPresenceProuveeService service;

    public AesPresenceProuveeController(AesPresenceProuveeService service) {
        this.service = service;
    }

    @PostMapping
    public AesPresenceProuveeResponse analyze(@PathVariable UUID caseFileId,
                                              @RequestBody AesPresenceProuveeRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AesPresenceProuveeResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
