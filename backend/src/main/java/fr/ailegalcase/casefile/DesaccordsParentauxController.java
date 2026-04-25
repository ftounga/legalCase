package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-19-05 : endpoint REST désaccords parentaux (art. 373-2-10 Cciv).
 * Single-country FR DROIT_FAMILLE.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/desaccords-parentaux")
public class DesaccordsParentauxController {

    private final DesaccordsParentauxService service;

    public DesaccordsParentauxController(DesaccordsParentauxService service) {
        this.service = service;
    }

    @PostMapping
    public DesaccordsParentauxResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody DesaccordsParentauxRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DesaccordsParentauxResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
