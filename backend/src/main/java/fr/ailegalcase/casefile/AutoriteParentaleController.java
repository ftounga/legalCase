package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-19-01 : endpoint REST autorité parentale (art. 372-373 Cciv).
 * Single-country FR DROIT_FAMILLE.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/autorite-parentale")
public class AutoriteParentaleController {

    private final AutoriteParentaleService service;

    public AutoriteParentaleController(AutoriteParentaleService service) {
        this.service = service;
    }

    @PostMapping
    public AutoriteParentaleResponse calculate(@PathVariable UUID caseFileId,
                                               @RequestBody AutoriteParentaleRequest request,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AutoriteParentaleResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
