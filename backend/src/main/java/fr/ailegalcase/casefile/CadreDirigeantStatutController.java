package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-19 : endpoints POST/GET pour l'analyse de qualification du cadre
 * dirigeant (art. L.3111-2 CT — 3 critères cumulatifs, F-DT-107). Outil
 * single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/cadre-dirigeant-statut-analysis")
public class CadreDirigeantStatutController {

    private final CadreDirigeantStatutService service;

    public CadreDirigeantStatutController(CadreDirigeantStatutService service) {
        this.service = service;
    }

    @PostMapping
    public CadreDirigeantStatutResponse analyze(@PathVariable UUID caseFileId,
                                                @RequestBody CadreDirigeantStatutRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CadreDirigeantStatutResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
