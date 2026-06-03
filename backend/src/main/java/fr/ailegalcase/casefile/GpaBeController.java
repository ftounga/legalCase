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
 * SF-223-04 : endpoints REST pour le cadrage de la situation contentieuse
 * post-GPA en Belgique (vide juridique — à vérifier par avocat belge).
 * POST/GET /api/v1/case-files/{caseFileId}/gpa-be-situation-contentieuse-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/gpa-be-situation-contentieuse-analysis")
public class GpaBeController {

    private final GpaBeService service;

    public GpaBeController(GpaBeService service) {
        this.service = service;
    }

    @PostMapping
    public GpaBeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody GpaBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public GpaBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
