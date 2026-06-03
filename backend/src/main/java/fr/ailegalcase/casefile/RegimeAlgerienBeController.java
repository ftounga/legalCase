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
 * SF-223-05 : endpoints REST pour le cadrage du corridor algérien BE
 * (reconnaissance mariage / talaq / dot relevant du droit algérien — CDIP,
 * Convention algéro-belge — à vérifier par avocat belge).
 * POST/GET /api/v1/case-files/{caseFileId}/regime-algerien-be-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/regime-algerien-be-analysis")
public class RegimeAlgerienBeController {

    private final RegimeAlgerienBeService service;

    public RegimeAlgerienBeController(RegimeAlgerienBeService service) {
        this.service = service;
    }

    @PostMapping
    public RegimeAlgerienBeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody RegimeAlgerienBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RegimeAlgerienBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
