package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-IA-04-01 : endpoint de lecture de la visibilité des outils décisionnels
 * pour un dossier. Pas d'écriture en V1 (les règles sont alimentées par
 * migrations Liquibase).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools-visibility")
public class DecisionToolVisibilityController {

    private final DecisionToolVisibilityService service;

    public DecisionToolVisibilityController(DecisionToolVisibilityService service) {
        this.service = service;
    }

    @GetMapping
    public VisibleToolSetResponse resolve(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.resolveVisibleTools(caseFileId, oidcUser, principal);
    }
}
