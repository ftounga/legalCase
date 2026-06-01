package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-35 : endpoints POST/GET pour l'analyse de validité d'un règlement
 * intérieur (art. L.1311-1 à L.1322-4, L.1321-1 et s. CT, F-DT-100). Outil
 * single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/reglement-interieur-validite-analysis")
public class ReglementInterieurValiditeController {

    private final ReglementInterieurValiditeService service;

    public ReglementInterieurValiditeController(ReglementInterieurValiditeService service) {
        this.service = service;
    }

    @PostMapping
    public ReglementInterieurValiditeResponse analyze(@PathVariable UUID caseFileId,
                                                      @RequestBody ReglementInterieurValiditeRequest request,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ReglementInterieurValiditeResponse get(@PathVariable UUID caseFileId,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
