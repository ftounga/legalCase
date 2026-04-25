package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-15-01 : controller pour le calcul des récompenses (art. 1437 et s. Cciv).
 * Outil single-country FR DROIT_FAMILLE.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/recompenses")
public class RecompensesController {

    private final RecompensesService service;

    public RecompensesController(RecompensesService service) {
        this.service = service;
    }

    @PostMapping
    public RecompensesResponse calculate(@PathVariable UUID caseFileId,
                                         @RequestBody RecompensesRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RecompensesResponse get(@PathVariable UUID caseFileId,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
