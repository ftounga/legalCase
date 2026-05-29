package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-21 : endpoints POST/GET pour l'analyse d'éligibilité au titre victime
 * de la traite des êtres humains L. 425-1 CESEDA. Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/victime-traite-analysis")
public class VictimeTraiteController {

    private final VictimeTraiteService service;

    public VictimeTraiteController(VictimeTraiteService service) {
        this.service = service;
    }

    @PostMapping
    public VictimeTraiteResponse analyze(@PathVariable UUID caseFileId,
                                         @RequestBody VictimeTraiteRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public VictimeTraiteResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
