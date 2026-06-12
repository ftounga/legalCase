package fr.ailegalcase.casefile.strategy;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-286 / SF-286-01 — endpoints de la stratégie de dossier unifiée.
 *
 * <p>{@code GET} lit la stratégie courante ; {@code POST} la (re)génère (couche LLM de
 * synthèse en lecture des verdicts calculés + synthèse). Isolation workspace via le
 * dossier (cf. {@link CaseStrategyService}).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/strategy")
public class CaseStrategyController {

    private final CaseStrategyService service;

    public CaseStrategyController(CaseStrategyService service) {
        this.service = service;
    }

    @GetMapping
    public CaseStrategyResponse get(@PathVariable UUID caseFileId,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }

    @PostMapping
    public CaseStrategyResponse generate(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.generate(caseFileId, oidcUser, principal);
    }
}
