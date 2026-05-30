package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-05 : endpoints POST/GET pour l'analyse d'un pourvoi en cassation
 * devant la chambre sociale (art. 612 CPC ; art. 604 CPC ; art. 973 CPC ;
 * art. 1014 CPC). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/pourvoi-cassation-soc-analysis")
public class PourvoiCassationSocController {

    private final PourvoiCassationSocService service;

    public PourvoiCassationSocController(PourvoiCassationSocService service) {
        this.service = service;
    }

    @PostMapping
    public PourvoiCassationSocResponse analyze(@PathVariable UUID caseFileId,
                                               @RequestBody PourvoiCassationSocRequest request,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PourvoiCassationSocResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
