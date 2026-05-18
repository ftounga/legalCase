package fr.ailegalcase.analysis;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-179 SF-179-01 — expose les vérifications de jurisprudence citée d'un dossier.
 */
@RestController
public class JurisprudenceCheckController {

    private final JurisprudenceCheckQueryService jurisprudenceCheckQueryService;

    public JurisprudenceCheckController(JurisprudenceCheckQueryService jurisprudenceCheckQueryService) {
        this.jurisprudenceCheckQueryService = jurisprudenceCheckQueryService;
    }

    /**
     * Renvoie les vérifications de jurisprudence de la dernière analyse
     * {@code DONE} du dossier. {@code checks} est vide si aucune référence n'a
     * été détectée.
     */
    @GetMapping("/api/v1/case-files/{caseFileId}/jurisprudence-checks")
    public JurisprudenceCheckResponse list(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return jurisprudenceCheckQueryService.getForCaseFile(caseFileId, oidcUser, principal);
    }
}
