package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-47 : endpoints POST/GET pour l'analyse du congé de proche aidant
 * (art. L.3142-16 à L.3142-27 CT, F-DT-79). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/conge-proche-aidant-analysis")
public class CongeProcheAidantController {

    private final CongeProcheAidantService service;

    public CongeProcheAidantController(CongeProcheAidantService service) {
        this.service = service;
    }

    @PostMapping
    public CongeProcheAidantResponse analyze(@PathVariable UUID caseFileId,
                                             @RequestBody CongeProcheAidantRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CongeProcheAidantResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
