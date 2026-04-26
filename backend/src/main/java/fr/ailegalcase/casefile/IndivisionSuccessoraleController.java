package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-24-11 : endpoint REST de l'outil "Indivision successorale" (art. 815
 * à 832-2 + 1873-1 et s. Cciv). Single-country FR DROIT_FAMILLE.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/indivision-successorale-analysis")
public class IndivisionSuccessoraleController {

    private final IndivisionSuccessoraleService service;

    public IndivisionSuccessoraleController(IndivisionSuccessoraleService service) {
        this.service = service;
    }

    @PostMapping
    public IndivisionSuccessoraleResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody IndivisionSuccessoraleRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public IndivisionSuccessoraleResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
