package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-18-09 : endpoints REST pour l'outil "Adoption"
 * (FR — DROIT_FAMILLE — art. 343-370-2 Cciv).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/adoption-analysis")
public class AdoptionController {

    private final AdoptionService service;

    public AdoptionController(AdoptionService service) {
        this.service = service;
    }

    @PostMapping
    public AdoptionResponse calculate(@PathVariable UUID caseFileId,
                                      @RequestBody AdoptionRequest request,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AdoptionResponse get(@PathVariable UUID caseFileId,
                                @AuthenticationPrincipal OidcUser oidcUser,
                                Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
