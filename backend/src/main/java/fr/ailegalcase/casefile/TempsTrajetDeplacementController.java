package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-51 : endpoints POST/GET pour l'outil "Temps de trajet / déplacement
 * professionnel" (art. L.3121-4 CT ; CJUE C-266/14, F-DT-81). Outil
 * single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/temps-trajet-deplacement-analysis")
public class TempsTrajetDeplacementController {

    private final TempsTrajetDeplacementService service;

    public TempsTrajetDeplacementController(TempsTrajetDeplacementService service) {
        this.service = service;
    }

    @PostMapping
    public TempsTrajetDeplacementResponse analyze(@PathVariable UUID caseFileId,
                                                  @RequestBody TempsTrajetDeplacementRequest request,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public TempsTrajetDeplacementResponse get(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
