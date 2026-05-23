package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-27 : endpoint REST pour l'outil Partage successoral notarié FR
 * (art. 816 et s. Cciv). POST/GET
 * /api/v1/case-files/{caseFileId}/partage-notarial.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/partage-notarial")
public class PartageNotarialController {

    private final PartageNotarialService service;

    public PartageNotarialController(PartageNotarialService service) {
        this.service = service;
    }

    @PostMapping
    public PartageNotarialResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody PartageNotarialRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PartageNotarialResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
