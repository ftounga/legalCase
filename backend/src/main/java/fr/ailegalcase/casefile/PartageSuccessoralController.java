package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-24-09 : endpoints REST pour l'outil "Partage successoral"
 * (FR — DROIT_FAMILLE — art. 815-840 Cciv + 1364 CPC).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/partage-successoral-analysis")
public class PartageSuccessoralController {

    private final PartageSuccessoralService service;

    public PartageSuccessoralController(PartageSuccessoralService service) {
        this.service = service;
    }

    @PostMapping
    public PartageSuccessoralResponse calculate(@PathVariable UUID caseFileId,
                                                @RequestBody PartageSuccessoralRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PartageSuccessoralResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
