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
 * SF-223-03 : endpoints REST pour l'analyse du recueil légal (kafala) en
 * Belgique (CDIP — loi du 16/07/2004 ; CC art. 343 al. 2 — à vérifier).
 * POST/GET /api/v1/case-files/{caseFileId}/kafala-be-recueil-legal-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/kafala-be-recueil-legal-analysis")
public class KafalaBeController {

    private final KafalaBeService service;

    public KafalaBeController(KafalaBeService service) {
        this.service = service;
    }

    @PostMapping
    public KafalaBeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody KafalaBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public KafalaBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
