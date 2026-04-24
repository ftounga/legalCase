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
 * SF-DT-11-01 : endpoints REST pour l'indemnité de licenciement nul
 * (harcèlement / discrimination / violence au travail — FR + BE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/harcelement-licenciement-nul")
public class HarcelementNulliteController {

    private final HarcelementNulliteService service;

    public HarcelementNulliteController(HarcelementNulliteService service) {
        this.service = service;
    }

    @PostMapping
    public HarcelementNulliteResponse calculate(@PathVariable UUID caseFileId,
                                                @RequestBody HarcelementNulliteRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public HarcelementNulliteResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
