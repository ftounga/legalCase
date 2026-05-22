package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-11 : endpoint REST pour l'outil retrait autorité parentale FR
 * (art. 378-381 Cciv + loi 2022-140 LMVSS).
 *
 * <p>POST/GET /api/v1/case-files/{caseFileId}/retrait-autorite-parentale.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/retrait-autorite-parentale")
public class RetraitAutoriteParentaleController {

    private final RetraitAutoriteParentaleService service;

    public RetraitAutoriteParentaleController(RetraitAutoriteParentaleService service) {
        this.service = service;
    }

    @PostMapping
    public RetraitAutoriteParentaleResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody RetraitAutoriteParentaleRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RetraitAutoriteParentaleResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
