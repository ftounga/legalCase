package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * F-98 / SF-98-01 — API du générateur de projet de conclusions.
 *
 * <p>Contrat figé :
 * <ul>
 *   <li>{@code POST .../conclusions/generate} → {@code 202 {"status":"PENDING"}}
 *       ; {@code 409} (gardes) ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code GET .../conclusions} → {@code 200 ConclusionResponse}
 *       ; {@code 404} ; {@code 401}.</li>
 * </ul>
 * Le controller ne porte aucune logique métier : il délègue à
 * {@link CaseConclusionCommandService}.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/conclusions")
public class CaseConclusionController {

    private final CaseConclusionCommandService caseConclusionCommandService;

    public CaseConclusionController(CaseConclusionCommandService caseConclusionCommandService) {
        this.caseConclusionCommandService = caseConclusionCommandService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ConclusionGenerationResponse> generate(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        ConclusionGenerationResponse body = caseConclusionCommandService.triggerGeneration(
                caseFileId, oidcUser, OAuthProviderResolver.resolve(principal), principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @GetMapping
    public ConclusionResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return caseConclusionCommandService.getConclusion(
                caseFileId, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
