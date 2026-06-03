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
 * SF-223-02 : endpoints REST pour l'analyse de recevabilité de l'adoption en
 * Belgique (loi du 24/04/2003 ; CC art. 343-1 et s. — à vérifier).
 * POST/GET /api/v1/case-files/{caseFileId}/adoption-be-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/adoption-be-analysis")
public class AdoptionBeController {

    private final AdoptionBeService service;

    public AdoptionBeController(AdoptionBeService service) {
        this.service = service;
    }

    @PostMapping
    public AdoptionBeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody AdoptionBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AdoptionBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
