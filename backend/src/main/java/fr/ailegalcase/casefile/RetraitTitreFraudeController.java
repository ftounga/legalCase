package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-41 : endpoints POST/GET pour l'analyse de validité d'un retrait de titre
 * de séjour pour fraude (art. L. 412-7 CESEDA). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/retrait-titre-fraude-analysis")
public class RetraitTitreFraudeController {

    private final RetraitTitreFraudeService service;

    public RetraitTitreFraudeController(RetraitTitreFraudeService service) {
        this.service = service;
    }

    @PostMapping
    public RetraitTitreFraudeResponse analyze(@PathVariable UUID caseFileId,
                                              @RequestBody RetraitTitreFraudeRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RetraitTitreFraudeResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
