package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-07 : endpoint REST pour l'outil ARIPA recouvrement FR (art. L. 581+
 * CSS). POST/GET /api/v1/case-files/{caseFileId}/aripa-recouvrement-fr.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/aripa-recouvrement-fr")
public class AripaRecouvrementFrController {

    private final AripaRecouvrementFrService service;

    public AripaRecouvrementFrController(AripaRecouvrementFrService service) {
        this.service = service;
    }

    @PostMapping
    public AripaRecouvrementFrResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody AripaRecouvrementFrRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AripaRecouvrementFrResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
