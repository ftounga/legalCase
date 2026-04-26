package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-IM-12-01 : controller HTTP pour l'analyse d'asile avancé (CESEDA Livre V — France).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/asile-avance-analysis")
public class AsileAvanceController {

    private final AsileAvanceService service;

    public AsileAvanceController(AsileAvanceService service) {
        this.service = service;
    }

    @PostMapping
    public AsileAvanceResponse calculate(@PathVariable UUID caseFileId,
                                         @RequestBody AsileAvanceRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AsileAvanceResponse get(@PathVariable UUID caseFileId,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
