package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-25 : endpoints POST/GET pour l'analyse des démarches ANEF et des recours
 * en cas de panne du dépôt dématérialisé (R. 311-2-2 CESEDA). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/anef-procedure-analysis")
public class AnefProcedureController {

    private final AnefProcedureService service;

    public AnefProcedureController(AnefProcedureService service) {
        this.service = service;
    }

    @PostMapping
    public AnefProcedureResponse analyze(@PathVariable UUID caseFileId,
                                         @RequestBody AnefProcedureRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AnefProcedureResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
