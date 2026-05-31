package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-25 : endpoints POST/GET pour l'analyse du licenciement pour fin de
 * chantier / d'opération du CDI de chantier (art. L.1223-8 et s. ; L.1236-8 CT,
 * F-DT-37). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/cdi-chantier-analysis")
public class CdiChantierController {

    private final CdiChantierService service;

    public CdiChantierController(CdiChantierService service) {
        this.service = service;
    }

    @PostMapping
    public CdiChantierResponse analyze(@PathVariable UUID caseFileId,
                                       @RequestBody CdiChantierRequest request,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CdiChantierResponse get(@PathVariable UUID caseFileId,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
