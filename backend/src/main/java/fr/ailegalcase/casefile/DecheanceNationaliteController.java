package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-220-05 : endpoints POST/GET pour l'analyse de validité d'une mesure de
 * déchéance de nationalité (Cciv 25 / 25-1,
 * F-IM-51-decheance-nationalite-fr). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decheance-nationalite-analysis")
public class DecheanceNationaliteController {

    private final DecheanceNationaliteService service;

    public DecheanceNationaliteController(DecheanceNationaliteService service) {
        this.service = service;
    }

    @PostMapping
    public DecheanceNationaliteResponse analyze(@PathVariable UUID caseFileId,
                                                @RequestBody DecheanceNationaliteRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DecheanceNationaliteResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
