package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-215-05 : endpoints REST de l'outil regroupement familial 10bis BE
 * (Loi 15/12/1980 art. 10 et 10bis — ressortissant tiers en séjour limité,
 * carte A).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/regroupement-10bis-be-analysis")
public class Regroupement10bisBeController {

    private final Regroupement10bisBeService service;

    public Regroupement10bisBeController(Regroupement10bisBeService service) {
        this.service = service;
    }

    @PostMapping
    public Regroupement10bisBeResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody Regroupement10bisBeRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public Regroupement10bisBeResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
