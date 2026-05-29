package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-29 : endpoints POST/GET pour l'analyse du délai de recours devant le
 * Tribunal judiciaire contre un refus de déclaration de nationalité française
 * (Cciv 26-3, délai 6 mois). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/naturalisation-recours-tj-analysis")
public class NaturalisationRecoursTjController {

    private final NaturalisationRecoursTjService service;

    public NaturalisationRecoursTjController(NaturalisationRecoursTjService service) {
        this.service = service;
    }

    @PostMapping
    public NaturalisationRecoursTjResponse analyze(@PathVariable UUID caseFileId,
                                                   @RequestBody NaturalisationRecoursTjRequest request,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public NaturalisationRecoursTjResponse get(@PathVariable UUID caseFileId,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
