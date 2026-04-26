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
 * SF-FA-16-01 : endpoints REST pour l'outil "Communauté universelle"
 * (FR — DROIT_FAMILLE — art. 1526 + 1527 al. 2 Cciv).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/communaute-universelle-analysis")
public class CommunauteUniverselleController {

    private final CommunauteUniverselleService service;

    public CommunauteUniverselleController(CommunauteUniverselleService service) {
        this.service = service;
    }

    @PostMapping
    public CommunauteUniverselleResponse calculate(@PathVariable UUID caseFileId,
                                                   @RequestBody CommunauteUniverselleRequest request,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CommunauteUniverselleResponse get(@PathVariable UUID caseFileId,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
