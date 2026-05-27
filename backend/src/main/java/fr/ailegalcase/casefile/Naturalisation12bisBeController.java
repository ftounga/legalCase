package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-215-07 : endpoints REST de l'outil naturalisation 12bis BE
 * (Code de la nationalité belge — loi 28/06/1984 consolidée art. 12bis).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/naturalisation-12bis-be-analysis")
public class Naturalisation12bisBeController {

    private final Naturalisation12bisBeService service;

    public Naturalisation12bisBeController(Naturalisation12bisBeService service) {
        this.service = service;
    }

    @PostMapping
    public Naturalisation12bisBeResponse calculate(@PathVariable UUID caseFileId,
                                                  @RequestBody Naturalisation12bisBeRequest request,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public Naturalisation12bisBeResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
