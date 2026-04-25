package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-IM-11-01 : controller HTTP pour l'analyse du changement de statut CESEDA.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/changement-statut-analysis")
public class ChangementStatutController {

    private final ChangementStatutService service;

    public ChangementStatutController(ChangementStatutService service) {
        this.service = service;
    }

    @PostMapping
    public ChangementStatutResponse calculate(@PathVariable UUID caseFileId,
                                              @RequestBody ChangementStatutRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ChangementStatutResponse get(@PathVariable UUID caseFileId,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
