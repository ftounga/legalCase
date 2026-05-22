package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-09 : endpoint REST pour l'outil délégation autorité parentale FR
 * (art. 376-1 Cciv).
 *
 * <p>POST/GET /api/v1/case-files/{caseFileId}/delegation-ap-fr.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/delegation-ap-fr")
public class DelegationApFrController {

    private final DelegationApFrService service;

    public DelegationApFrController(DelegationApFrService service) {
        this.service = service;
    }

    @PostMapping
    public DelegationApFrResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody DelegationApFrRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DelegationApFrResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
