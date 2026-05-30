package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-13 : endpoints POST/GET pour l'analyse de la rupture du contrat d'un
 * salarié du particulier employeur (CESU / assistant maternel) — préavis et
 * indemnité de licenciement / de rupture (F-DT-108). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/particulier-employeur-cesu-analysis")
public class ParticulierEmployeurCesuController {

    private final ParticulierEmployeurCesuService service;

    public ParticulierEmployeurCesuController(ParticulierEmployeurCesuService service) {
        this.service = service;
    }

    @PostMapping
    public ParticulierEmployeurCesuResponse analyze(@PathVariable UUID caseFileId,
                                                    @RequestBody ParticulierEmployeurCesuRequest request,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ParticulierEmployeurCesuResponse get(@PathVariable UUID caseFileId,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
