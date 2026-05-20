package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-01 : endpoints POST/GET pour l'analyse étranger malade L. 425-9 CESEDA.
 * Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/etranger-malade-analysis")
public class EtrangerMaladeController {

    private final EtrangerMaladeService service;

    public EtrangerMaladeController(EtrangerMaladeService service) {
        this.service = service;
    }

    @PostMapping
    public EtrangerMaladeResponse analyze(@PathVariable UUID caseFileId,
                                           @RequestBody EtrangerMaladeRequest request,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public EtrangerMaladeResponse get(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
