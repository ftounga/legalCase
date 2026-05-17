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
 * SF-217-02 : endpoints REST pour le suivi de la procédure de liquidation-partage
 * post-divorce belge (notaire commis — BELGIQUE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/liquidation-partage-be")
public class LiquidationPartageBeController {

    private final LiquidationPartageBeService service;

    public LiquidationPartageBeController(LiquidationPartageBeService service) {
        this.service = service;
    }

    @PostMapping
    public LiquidationPartageBeResponse calculate(@PathVariable UUID caseFileId,
                                                  @RequestBody LiquidationPartageBeRequest request,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LiquidationPartageBeResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
