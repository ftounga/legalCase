package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-19 : endpoints POST/GET pour l'analyse d'éligibilité à l'aide
 * juridictionnelle (AJ) devant la CNDA et des délais (loi n° 91-647, L. 532-4
 * CESEDA). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/aj-cnda-analysis")
public class AjCndaController {

    private final AjCndaService service;

    public AjCndaController(AjCndaService service) {
        this.service = service;
    }

    @PostMapping
    public AjCndaResponse analyze(@PathVariable UUID caseFileId,
                                  @RequestBody AjCndaRequest request,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AjCndaResponse get(@PathVariable UUID caseFileId,
                              @AuthenticationPrincipal OidcUser oidcUser,
                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
