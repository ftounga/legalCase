package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-05 : endpoint REST pour la liquidation de communauté légale FR
 * (art. 1467-1517 Cciv).
 * POST/GET /api/v1/case-files/{caseFileId}/liquidation-communaute-fr
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/liquidation-communaute-fr")
public class LiquidationCommunauteFrController {

    private final LiquidationCommunauteFrService service;

    public LiquidationCommunauteFrController(LiquidationCommunauteFrService service) {
        this.service = service;
    }

    @PostMapping
    public LiquidationCommunauteFrResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody LiquidationCommunauteFrRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LiquidationCommunauteFrResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
