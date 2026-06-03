package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-41 : endpoints POST/GET pour l'analyse de conformité aux obligations
 * d'épargne salariale (intéressement / participation / dispositif de partage de
 * la valeur — art. L.3311-1 et s., L.3321-1 et s., L.3322-2 CT + loi n° 2023-1107
 * du 29/11/2023, F-DT-53). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/epargne-salariale-conformite-analysis")
public class EpargneSalarialeConformiteController {

    private final EpargneSalarialeConformiteService service;

    public EpargneSalarialeConformiteController(EpargneSalarialeConformiteService service) {
        this.service = service;
    }

    @PostMapping
    public EpargneSalarialeConformiteResponse analyze(@PathVariable UUID caseFileId,
                                                      @RequestBody EpargneSalarialeConformiteRequest request,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public EpargneSalarialeConformiteResponse get(@PathVariable UUID caseFileId,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
