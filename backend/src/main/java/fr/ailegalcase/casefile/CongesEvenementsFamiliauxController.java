package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-43 : endpoints POST/GET pour l'analyse du congé pour évènement familial
 * (art. L.3142-1 à L.3142-5 CT, F-DT-76). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/conges-evenements-familiaux-analysis")
public class CongesEvenementsFamiliauxController {

    private final CongesEvenementsFamiliauxService service;

    public CongesEvenementsFamiliauxController(CongesEvenementsFamiliauxService service) {
        this.service = service;
    }

    @PostMapping
    public CongesEvenementsFamiliauxResponse analyze(@PathVariable UUID caseFileId,
                                                     @RequestBody CongesEvenementsFamiliauxRequest request,
                                                     @AuthenticationPrincipal OidcUser oidcUser,
                                                     Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CongesEvenementsFamiliauxResponse get(@PathVariable UUID caseFileId,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
