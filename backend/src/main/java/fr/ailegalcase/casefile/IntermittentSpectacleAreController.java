package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-17 : endpoints POST/GET pour l'analyse d'ouverture des droits à l'ARE
 * de l'intermittent du spectacle (annexes 8 et 10 Unedic — seuil 507 h / 12
 * mois, F-DT-106). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/intermittent-spectacle-are-analysis")
public class IntermittentSpectacleAreController {

    private final IntermittentSpectacleAreService service;

    public IntermittentSpectacleAreController(IntermittentSpectacleAreService service) {
        this.service = service;
    }

    @PostMapping
    public IntermittentSpectacleAreResponse analyze(@PathVariable UUID caseFileId,
                                                    @RequestBody IntermittentSpectacleAreRequest request,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public IntermittentSpectacleAreResponse get(@PathVariable UUID caseFileId,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
