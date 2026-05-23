package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-13 : endpoint REST pour l'outil Audition du mineur par le JAF FR
 * (art. 388-1 Cciv + art. 1074-1 à 1074-3 CPC). POST/GET
 * /api/v1/case-files/{caseFileId}/audition-mineur.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/audition-mineur")
public class AuditionMineurController {

    private final AuditionMineurService service;

    public AuditionMineurController(AuditionMineurService service) {
        this.service = service;
    }

    @PostMapping
    public AuditionMineurResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody AuditionMineurRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AuditionMineurResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
