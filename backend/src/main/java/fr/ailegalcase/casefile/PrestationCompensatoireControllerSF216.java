package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-01 : endpoint REST pour la prestation compensatoire (art. 270-281 Cciv).
 * POST/GET /api/v1/case-files/{caseFileId}/prestation-compensatoire
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/prestation-compensatoire")
public class PrestationCompensatoireControllerSF216 {

    private final PrestationCompensatoireServiceSF216 service;

    public PrestationCompensatoireControllerSF216(PrestationCompensatoireServiceSF216 service) {
        this.service = service;
    }

    @PostMapping
    public PrestationCompensatoireResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody PrestationCompensatoireRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PrestationCompensatoireResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
