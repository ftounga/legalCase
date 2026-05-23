package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-21 : endpoint REST pour l'outil Recel successoral FR
 * (art. 778 Cciv). POST/GET
 * /api/v1/case-files/{caseFileId}/recel-succession.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/recel-succession")
public class RecelSuccessionController {

    private final RecelSuccessionService service;

    public RecelSuccessionController(RecelSuccessionService service) {
        this.service = service;
    }

    @PostMapping
    public RecelSuccessionResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody RecelSuccessionRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RecelSuccessionResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
