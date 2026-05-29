package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-17 : endpoints POST/GET pour l'analyse de la procédure d'introduction de
 * la demande d'asile à l'OFPRA (GUDA / ADA, art. R. 521-1 et s. CESEDA).
 * Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/ofpra-introduction-analysis")
public class OfpraIntroductionController {

    private final OfpraIntroductionService service;

    public OfpraIntroductionController(OfpraIntroductionService service) {
        this.service = service;
    }

    @PostMapping
    public OfpraIntroductionResponse analyze(@PathVariable UUID caseFileId,
                                             @RequestBody OfpraIntroductionRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public OfpraIntroductionResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
