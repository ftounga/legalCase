package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-15 : endpoint REST pour l'outil Adoption intra-familiale FR
 * (art. 343-1 al. 2 + 345-1 Cciv). POST/GET
 * /api/v1/case-files/{caseFileId}/adoption-intra-fr.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/adoption-intra-fr")
public class AdoptionIntraFrController {

    private final AdoptionIntraFrService service;

    public AdoptionIntraFrController(AdoptionIntraFrService service) {
        this.service = service;
    }

    @PostMapping
    public AdoptionIntraFrResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody AdoptionIntraFrRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AdoptionIntraFrResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
