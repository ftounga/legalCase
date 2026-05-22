package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-03 : endpoint REST pour la pension alimentaire enfant FR
 * (art. 371-2 Cciv).
 * POST/GET /api/v1/case-files/{caseFileId}/pension-alimentaire-enfant-fr
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/pension-alimentaire-enfant-fr")
public class PensionAlimentaireEnfantFrController {

    private final PensionAlimentaireEnfantFrService service;

    public PensionAlimentaireEnfantFrController(PensionAlimentaireEnfantFrService service) {
        this.service = service;
    }

    @PostMapping
    public PensionAlimentaireEnfantFrResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody PensionAlimentaireEnfantFrRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PensionAlimentaireEnfantFrResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
