package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-05 : endpoints POST/GET pour l'analyse d'éligibilité à la VPF liens
 * personnels et familiaux L. 423-23 CESEDA (art. 8 CEDH). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/vpf-liens-personnels-analysis")
public class VpfLiensPersonnelsController {

    private final VpfLiensPersonnelsService service;

    public VpfLiensPersonnelsController(VpfLiensPersonnelsService service) {
        this.service = service;
    }

    @PostMapping
    public VpfLiensPersonnelsResponse analyze(@PathVariable UUID caseFileId,
                                              @RequestBody VpfLiensPersonnelsRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public VpfLiensPersonnelsResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
