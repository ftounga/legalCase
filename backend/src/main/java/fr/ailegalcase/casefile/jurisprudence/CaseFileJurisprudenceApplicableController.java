package fr.ailegalcase.casefile.jurisprudence;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-JU-02 / SF-JU-02-02 — endpoint d'exposition de la jurisprudence applicable
 * pour le frontend (section « 📚 Jurisprudence applicable » du PDF synthèse).
 *
 * <p>Contrat figé :
 * <ul>
 *   <li>{@code GET .../jurisprudence-applicable} →
 *       {@code 200 JurisprudenceApplicableResponse} ; {@code 404} ; {@code 401}.</li>
 * </ul>
 * Le controller ne porte aucune logique métier : il délègue à
 * {@link CaseFileJurisprudenceApplicableService}.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/jurisprudence-applicable")
public class CaseFileJurisprudenceApplicableController {

    private final CaseFileJurisprudenceApplicableService service;

    public CaseFileJurisprudenceApplicableController(CaseFileJurisprudenceApplicableService service) {
        this.service = service;
    }

    @GetMapping
    public JurisprudenceApplicableResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.getForCaseFile(
                caseFileId, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
