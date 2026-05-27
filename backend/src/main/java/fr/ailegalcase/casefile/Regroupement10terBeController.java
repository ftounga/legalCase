package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-215-03 : endpoints REST de l'outil regroupement familial 10ter BE
 * (Loi 15/12/1980 art. 10 et 10ter — ressortissant tiers en séjour illimité,
 * carte B ou C).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/regroupement-10ter-be-analysis")
public class Regroupement10terBeController {

    private final Regroupement10terBeService service;

    public Regroupement10terBeController(Regroupement10terBeService service) {
        this.service = service;
    }

    @PostMapping
    public Regroupement10terBeResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody Regroupement10terBeRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public Regroupement10terBeResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
