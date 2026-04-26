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
 * SF-FA-24-07 : endpoints REST pour l'outil "Réserve héréditaire et action en
 * réduction" (FR — DROIT_FAMILLE — art. 913 + 914-1 + 920-928 Cciv).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/reserve-heriditaire-analysis")
public class ReserveHereditaireController {

    private final ReserveHereditaireService service;

    public ReserveHereditaireController(ReserveHereditaireService service) {
        this.service = service;
    }

    @PostMapping
    public ReserveHereditaireResponse calculate(@PathVariable UUID caseFileId,
                                                @RequestBody ReserveHereditaireRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ReserveHereditaireResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
