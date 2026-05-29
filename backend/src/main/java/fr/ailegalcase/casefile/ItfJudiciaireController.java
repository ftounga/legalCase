package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-37 : endpoints POST/GET pour l'analyse d'une interdiction du territoire
 * français (ITF) prononcée par un juge pénal (C. pén. 131-30). Outil
 * single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/itf-judiciaire-analysis")
public class ItfJudiciaireController {

    private final ItfJudiciaireService service;

    public ItfJudiciaireController(ItfJudiciaireService service) {
        this.service = service;
    }

    @PostMapping
    public ItfJudiciaireResponse analyze(@PathVariable UUID caseFileId,
                                         @RequestBody ItfJudiciaireRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ItfJudiciaireResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
