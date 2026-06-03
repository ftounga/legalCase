package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-220-01 : endpoints POST/GET pour l'analyse du régime franco-tunisien
 * (accord du 17/03/1988). Outil single-country FR (F-IM-47-regime-tunisien-fr).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/regime-tunisien-analysis")
public class RegimeTunisienController {

    private final RegimeTunisienService service;

    public RegimeTunisienController(RegimeTunisienService service) {
        this.service = service;
    }

    @PostMapping
    public RegimeTunisienResponse analyze(@PathVariable UUID caseFileId,
                                          @RequestBody RegimeTunisienRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RegimeTunisienResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
