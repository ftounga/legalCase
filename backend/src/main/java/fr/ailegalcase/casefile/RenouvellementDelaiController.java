package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-13 : endpoints POST/GET pour l'analyse du délai de dépôt du
 * renouvellement du titre de séjour (2 mois avant expiration, art. R. 433-1
 * CESEDA). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/renouvellement-delai-analysis")
public class RenouvellementDelaiController {

    private final RenouvellementDelaiService service;

    public RenouvellementDelaiController(RenouvellementDelaiService service) {
        this.service = service;
    }

    @PostMapping
    public RenouvellementDelaiResponse analyze(@PathVariable UUID caseFileId,
                                               @RequestBody RenouvellementDelaiRequest request,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RenouvellementDelaiResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
