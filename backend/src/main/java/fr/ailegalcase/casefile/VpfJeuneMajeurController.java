package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-220-03 : endpoints POST/GET pour l'analyse d'éligibilité VPF jeune majeur
 * L.423-22 (F-IM-49-vpf-jeune-majeur-l42322-fr). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/vpf-jeune-majeur-analysis")
public class VpfJeuneMajeurController {

    private final VpfJeuneMajeurService service;

    public VpfJeuneMajeurController(VpfJeuneMajeurService service) {
        this.service = service;
    }

    @PostMapping
    public VpfJeuneMajeurResponse analyze(@PathVariable UUID caseFileId,
                                          @RequestBody VpfJeuneMajeurRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public VpfJeuneMajeurResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
