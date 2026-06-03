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
 * SF-223-01 : endpoints REST pour l'analyse du régime de la cohabitation légale
 * en Belgique (loi du 23/11/1998 ; CC art. 1475-1479 — à vérifier).
 * POST/GET /api/v1/case-files/{caseFileId}/cohabitation-legale-be-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/cohabitation-legale-be-analysis")
public class CohabitationLegaleBeController {

    private final CohabitationLegaleBeService service;

    public CohabitationLegaleBeController(CohabitationLegaleBeService service) {
        this.service = service;
    }

    @PostMapping
    public CohabitationLegaleBeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody CohabitationLegaleBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CohabitationLegaleBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
