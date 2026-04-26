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
 * SF-FA-18-05 : endpoints REST pour l'outil "Action en recherche de paternité"
 * (FR — DROIT_FAMILLE — art. 327 + 340 + 16-11 + 321 Cciv).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/recherche-paternite-analysis")
public class RecherchePaterniteController {

    private final RecherchePaterniteService service;

    public RecherchePaterniteController(RecherchePaterniteService service) {
        this.service = service;
    }

    @PostMapping
    public RecherchePaterniteResponse calculate(@PathVariable UUID caseFileId,
                                                @RequestBody RecherchePaterniteRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RecherchePaterniteResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
