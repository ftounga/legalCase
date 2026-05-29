package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-33 : endpoints POST/GET pour l'analyse des délais d'appel CAA / cassation
 * CE en contentieux des étrangers (art. L. 811-1 / R. 811-2 et L. 821-1 / R. 821-1
 * CJA). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/appel-caa-cassation-analysis")
public class AppelCaaCassationController {

    private final AppelCaaCassationService service;

    public AppelCaaCassationController(AppelCaaCassationService service) {
        this.service = service;
    }

    @PostMapping
    public AppelCaaCassationResponse analyze(@PathVariable UUID caseFileId,
                                             @RequestBody AppelCaaCassationRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AppelCaaCassationResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
