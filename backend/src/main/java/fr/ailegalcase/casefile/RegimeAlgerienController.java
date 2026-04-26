package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-IM-17-01 : controller HTTP pour l'analyse du régime franco-algérien
 * (accord du 27/12/1968 + avenants 1985 / 1994 / 2001).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/regime-algerien-analysis")
public class RegimeAlgerienController {

    private final RegimeAlgerienService service;

    public RegimeAlgerienController(RegimeAlgerienService service) {
        this.service = service;
    }

    @PostMapping
    public RegimeAlgerienResponse calculate(@PathVariable UUID caseFileId,
                                            @RequestBody RegimeAlgerienRequest request,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RegimeAlgerienResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
