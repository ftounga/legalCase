package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-17 : endpoint REST pour l'outil Adoption internationale FR
 * (art. 370-3 à 370-5 Cciv + Convention La Haye 1993). POST/GET
 * /api/v1/case-files/{caseFileId}/adoption-internationale.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/adoption-internationale")
public class AdoptionInternationaleController {

    private final AdoptionInternationaleService service;

    public AdoptionInternationaleController(AdoptionInternationaleService service) {
        this.service = service;
    }

    @PostMapping
    public AdoptionInternationaleResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody AdoptionInternationaleRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AdoptionInternationaleResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
