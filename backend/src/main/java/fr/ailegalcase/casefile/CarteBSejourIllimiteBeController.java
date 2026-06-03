package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-221-02 : endpoints REST de l'outil carte B séjour illimité BE
 * (Loi 15/12/1980 art. 14 — passage carte A → séjour illimité après 5 ans).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/carte-b-sejour-illimite-be-analysis")
public class CarteBSejourIllimiteBeController {

    private final CarteBSejourIllimiteBeService service;

    public CarteBSejourIllimiteBeController(CarteBSejourIllimiteBeService service) {
        this.service = service;
    }

    @PostMapping
    public CarteBSejourIllimiteBeResponse calculate(@PathVariable UUID caseFileId,
                                                    @RequestBody CarteBSejourIllimiteBeRequest request,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CarteBSejourIllimiteBeResponse get(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
