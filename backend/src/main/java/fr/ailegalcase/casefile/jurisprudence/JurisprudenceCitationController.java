package fr.ailegalcase.casefile.jurisprudence;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-242 / SF-242-01 — API CRUD des citations de jurisprudence d'appui d'un dossier.
 *
 * <p>Contrat figé (mini-spec SF-242-01) :
 * <ul>
 *   <li>{@code GET .../jurisprudence-citations} → {@code 200 JurisprudenceCitationListResponse}
 *       ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code POST .../jurisprudence-citations} → {@code 201 JurisprudenceCitationResponse}
 *       ; {@code 400} ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code PUT .../jurisprudence-citations/{citationId}} → {@code 200 JurisprudenceCitationResponse}
 *       ; {@code 400} ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code DELETE .../jurisprudence-citations/{citationId}} → {@code 204}
 *       ; {@code 404} ; {@code 401}.</li>
 * </ul>
 * Le controller ne porte aucune logique métier : il délègue à
 * {@link JurisprudenceCitationService}.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/jurisprudence-citations")
public class JurisprudenceCitationController {

    private final JurisprudenceCitationService jurisprudenceCitationService;

    public JurisprudenceCitationController(JurisprudenceCitationService jurisprudenceCitationService) {
        this.jurisprudenceCitationService = jurisprudenceCitationService;
    }

    @GetMapping
    public JurisprudenceCitationListResponse list(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return jurisprudenceCitationService.list(
                caseFileId, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @PostMapping
    public ResponseEntity<JurisprudenceCitationResponse> create(
            @PathVariable UUID caseFileId,
            @RequestBody CreateJurisprudenceCitationRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        JurisprudenceCitationResponse body = jurisprudenceCitationService.create(
                caseFileId, request, oidcUser, OAuthProviderResolver.resolve(principal), principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{citationId}")
    public JurisprudenceCitationResponse update(
            @PathVariable UUID caseFileId,
            @PathVariable UUID citationId,
            @RequestBody UpdateJurisprudenceCitationRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return jurisprudenceCitationService.update(
                caseFileId, citationId, request, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    @DeleteMapping("/{citationId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID caseFileId,
            @PathVariable UUID citationId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        jurisprudenceCitationService.delete(
                caseFileId, citationId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
        return ResponseEntity.noContent().build();
    }
}
