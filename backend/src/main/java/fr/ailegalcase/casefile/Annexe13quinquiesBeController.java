package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-215-17 : endpoints REST de l'outil "Annexe 13quinquies OQT + interdiction
 * d'entrée BE" (art. 74/11 Loi 15/12/1980). Outil BELGIQUE UNIQUEMENT (droit des
 * étrangers).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/annexe13quinquies-be-analysis")
public class Annexe13quinquiesBeController {

    private final Annexe13quinquiesBeService service;

    public Annexe13quinquiesBeController(Annexe13quinquiesBeService service) {
        this.service = service;
    }

    @PostMapping
    public Annexe13quinquiesBeResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody Annexe13quinquiesBeRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public Annexe13quinquiesBeResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
