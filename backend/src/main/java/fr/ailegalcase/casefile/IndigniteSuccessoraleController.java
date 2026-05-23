package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-19 : endpoint REST pour l'outil Indignité successorale FR
 * (art. 726-729-1 Cciv). POST/GET
 * /api/v1/case-files/{caseFileId}/indignite-successorale.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/indignite-successorale")
public class IndigniteSuccessoraleController {

    private final IndigniteSuccessoraleService service;

    public IndigniteSuccessoraleController(IndigniteSuccessoraleService service) {
        this.service = service;
    }

    @PostMapping
    public IndigniteSuccessoraleResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody IndigniteSuccessoraleRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public IndigniteSuccessoraleResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
