package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-22-01 : endpoint REST de l'outil "Indivision post-communautaire" (art.
 * 815 Cciv + 1364 CPC). Single-country FR DROIT_FAMILLE.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/indivision")
public class IndivisionController {

    private final IndivisionService service;

    public IndivisionController(IndivisionService service) {
        this.service = service;
    }

    @PostMapping
    public IndivisionResponse calculate(@PathVariable UUID caseFileId,
                                        @RequestBody IndivisionRequest request,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public IndivisionResponse get(@PathVariable UUID caseFileId,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
