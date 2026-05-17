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
 * SF-217-01 : endpoints REST pour l'analyse du régime de communauté légale belge
 * (composition du patrimoine du couple — BELGIQUE).
 *
 * <p>Le contrôleur ne porte aucune logique métier : il délègue au service, qui
 * lui-même délègue la qualification au {@code RegimeCommunauteLegaleBeCalculator}.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/regime-mat-be-communaute-legale")
public class RegimeCommunauteLegaleBeController {

    private final RegimeCommunauteLegaleBeService service;

    public RegimeCommunauteLegaleBeController(RegimeCommunauteLegaleBeService service) {
        this.service = service;
    }

    @PostMapping
    public RegimeCommunauteLegaleBeResponse calculate(@PathVariable UUID caseFileId,
                                                      @RequestBody RegimeCommunauteLegaleBeRequest request,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RegimeCommunauteLegaleBeResponse get(@PathVariable UUID caseFileId,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
