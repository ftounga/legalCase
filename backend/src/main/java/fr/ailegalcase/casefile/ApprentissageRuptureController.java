package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-23 : endpoints POST/GET pour l'analyse de la validité de la rupture du
 * contrat d'apprentissage (régime hybride 45 jours / motifs limités,
 * art. L.6222-18 et s. CT, F-DT-110). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/apprentissage-rupture-analysis")
public class ApprentissageRuptureController {

    private final ApprentissageRuptureService service;

    public ApprentissageRuptureController(ApprentissageRuptureService service) {
        this.service = service;
    }

    @PostMapping
    public ApprentissageRuptureResponse analyze(@PathVariable UUID caseFileId,
                                                @RequestBody ApprentissageRuptureRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ApprentissageRuptureResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
