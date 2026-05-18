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
 * SF-217-04 : endpoints REST pour l'outil décisionnel "Autorité parentale"
 * (Vague 2 Famille BE — CC art. 374-375, BELGIQUE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/autorite-parentale-be")
public class AutoriteParentaleBeController {

    private final AutoriteParentaleBeService service;

    public AutoriteParentaleBeController(AutoriteParentaleBeService service) {
        this.service = service;
    }

    @PostMapping
    public AutoriteParentaleBeResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody AutoriteParentaleBeRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AutoriteParentaleBeResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
