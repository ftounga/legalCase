package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-24-13 : endpoints REST pour l'outil "Rapport à succession" (FR —
 * DROIT_FAMILLE — art. 843-863 + 919 Cciv).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/rapport-succession-analysis")
public class RapportSuccessionController {

    private final RapportSuccessionService service;

    public RapportSuccessionController(RapportSuccessionService service) {
        this.service = service;
    }

    @PostMapping
    public RapportSuccessionResponse calculate(@PathVariable UUID caseFileId,
                                               @RequestBody RapportSuccessionRequest request,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RapportSuccessionResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
