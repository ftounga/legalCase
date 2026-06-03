package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-39 : endpoints POST/GET pour l'analyse d'exonération de la prime de
 * partage de la valeur (PPV — loi n° 2022-1158 du 16/08/2022 art. 1 + loi
 * n° 2023-1107 du 29/11/2023, F-DT-52). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/ppv-exoneration-analysis")
public class PpvExonerationController {

    private final PpvExonerationService service;

    public PpvExonerationController(PpvExonerationService service) {
        this.service = service;
    }

    @PostMapping
    public PpvExonerationResponse analyze(@PathVariable UUID caseFileId,
                                          @RequestBody PpvExonerationRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PpvExonerationResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
