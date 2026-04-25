package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-19-03 : endpoint REST changement de résidence enfant —
 * déménagement parent (art. 373-2 al. 3 Cciv).
 * Single-country FR DROIT_FAMILLE.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/changement-residence")
public class ChangementResidenceController {

    private final ChangementResidenceService service;

    public ChangementResidenceController(ChangementResidenceService service) {
        this.service = service;
    }

    @PostMapping
    public ChangementResidenceResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody ChangementResidenceRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ChangementResidenceResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
