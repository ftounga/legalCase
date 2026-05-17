package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-98 / SF-98-01 + SF-98-52 — API du générateur de projet de conclusions versionné.
 *
 * <p>Contrat figé :
 * <ul>
 *   <li>{@code POST .../conclusions/generate} → {@code 202 {"status":"PENDING","versionNumber":N}}
 *       ; {@code 409} ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code GET .../conclusions} → {@code 200 ConclusionResponse} (version la plus récente)
 *       ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code GET .../conclusions/versions} → {@code 200 ConclusionVersionSummary[]}
 *       ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code GET .../conclusions/versions/{versionId}} → {@code 200 ConclusionResponse}
 *       ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code PATCH .../conclusions/versions/{versionId}/lifecycle} → {@code 200 ConclusionResponse}
 *       ; {@code 400} ; {@code 409} ; {@code 404} ; {@code 401}.</li>
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

    @GetMapping("/versions")
    public List<ConclusionVersionSummary> listVersions(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return caseConclusionCommandService.listVersions(
                caseFileId, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/versions/{versionId}")
    public ConclusionResponse getVersion(
            @PathVariable UUID caseFileId,
            @PathVariable UUID versionId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return caseConclusionCommandService.getVersion(
                caseFileId, versionId, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @PatchMapping("/versions/{versionId}/lifecycle")
    public ConclusionResponse updateLifecycle(
            @PathVariable UUID caseFileId,
            @PathVariable UUID versionId,
            @RequestBody LifecycleUpdateRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return caseConclusionCommandService.updateLifecycle(
                caseFileId, versionId, request != null ? request.lifecycleStatus() : null,
                oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
