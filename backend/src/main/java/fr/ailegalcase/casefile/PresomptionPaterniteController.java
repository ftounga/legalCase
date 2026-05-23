package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-25 : endpoint REST pour l'outil Présomption de paternité du
 * mari et désaveu FR (art. 312-315 Cciv + art. 316 al. 2 + art. 333
 * al. 1). POST/GET
 * /api/v1/case-files/{caseFileId}/presomption-paternite.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/presomption-paternite")
public class PresomptionPaterniteController {

    private final PresomptionPaterniteService service;

    public PresomptionPaterniteController(PresomptionPaterniteService service) {
        this.service = service;
    }

    @PostMapping
    public PresomptionPaterniteResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody PresomptionPaterniteRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PresomptionPaterniteResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
