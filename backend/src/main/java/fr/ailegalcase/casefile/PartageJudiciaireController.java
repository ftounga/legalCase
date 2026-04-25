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
 * SF-FA-17-01 : endpoints REST pour l'outil "Partage judiciaire"
 * (FR — DROIT_FAMILLE — art. 840 et s. Cciv + 1364 et s. CPC).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/partage-judiciaire-analysis")
public class PartageJudiciaireController {

    private final PartageJudiciaireService service;

    public PartageJudiciaireController(PartageJudiciaireService service) {
        this.service = service;
    }

    @PostMapping
    public PartageJudiciaireResponse calculate(@PathVariable UUID caseFileId,
                                               @RequestBody PartageJudiciaireRequest request,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PartageJudiciaireResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
