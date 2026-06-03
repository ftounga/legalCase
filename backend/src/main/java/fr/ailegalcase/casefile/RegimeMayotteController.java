package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-220-02 : endpoints POST/GET pour l'analyse de portée territoriale du titre
 * mahorais. Outil single-country FR (F-IM-48-regime-mayotte-fr).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/regime-mayotte-analysis")
public class RegimeMayotteController {

    private final RegimeMayotteService service;

    public RegimeMayotteController(RegimeMayotteService service) {
        this.service = service;
    }

    @PostMapping
    public RegimeMayotteResponse analyze(@PathVariable UUID caseFileId,
                                         @RequestBody RegimeMayotteRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RegimeMayotteResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
