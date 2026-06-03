package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-222-02 : endpoint REST pour l'outil TGD (téléphone grave danger —
 * éligibilité, art. 41-3-1 CPP — F-FA-TGD, FRANCE).
 * POST/GET /api/v1/case-files/{caseFileId}/tgd-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/tgd-analysis")
public class TgdController {

    private final TgdService service;

    public TgdController(TgdService service) {
        this.service = service;
    }

    @PostMapping
    public TgdResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody TgdRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public TgdResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
