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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-98 / SF-98-01 + SF-98-52 — API du générateur de projet de conclusions versionné.
 *
 * <p>Contrat figé :
 * <ul>
 *   <li>{@code POST .../conclusions/generate?fromScratch={bool}} → {@code 202 {"status":"PENDING","versionNumber":N}}
 *       ; {@code 409} ; {@code 404} ; {@code 401}. {@code fromScratch} défaut {@code false}
 *       (« Actualiser ») ; {@code true} = « Régénérer de zéro » (SF-271-02).</li>
 *   <li>{@code GET .../conclusions} → {@code 200 ConclusionResponse} (version la plus récente)
 *       ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code GET .../conclusions/versions} → {@code 200 ConclusionVersionSummary[]}
 *       ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code GET .../conclusions/versions/{versionId}} → {@code 200 ConclusionResponse}
 *       ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code PATCH .../conclusions/versions/{versionId}/lifecycle} → {@code 200 ConclusionResponse}
 *       ; {@code 400} ; {@code 409} ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code PATCH .../conclusions/versions/{versionId}/content} → {@code 200 ConclusionResponse}
 *       ; {@code 400} ; {@code 409} ; {@code 404} ; {@code 401} (SF-98-49).</li>
 * </ul>
 * Le controller ne porte aucune logique métier : il délègue à
 * {@link CaseConclusionCommandService}.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/conclusions")
public class CaseConclusionController {

    private final CaseConclusionCommandService caseConclusionCommandService;
    private final ConclusionSectionRegenerationService sectionRegenerationService;

    public CaseConclusionController(CaseConclusionCommandService caseConclusionCommandService,
                                    ConclusionSectionRegenerationService sectionRegenerationService) {
        this.caseConclusionCommandService = caseConclusionCommandService;
        this.sectionRegenerationService = sectionRegenerationService;
    }

    /**
     * SF-271-02 — déclenche une nouvelle version de conclusions.
     *
     * <p>{@code fromScratch} (défaut {@code false}) sélectionne le mode :
     * <ul>
     *   <li>{@code false} = « Actualiser » : le worker repart de la dernière version
     *       DONE (bordereau retiré) comme texte à préserver ;</li>
     *   <li>{@code true} = « Régénérer de zéro » : le worker ignore la base
     *       récapitulative (génération from-scratch depuis l'analyse).</li>
     * </ul>
     * Le défaut préserve le contrat des appelants existants.</p>
     */
    @PostMapping("/generate")
    public ResponseEntity<ConclusionGenerationResponse> generate(
            @PathVariable UUID caseFileId,
            @RequestParam(defaultValue = "false") boolean fromScratch,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        ConclusionGenerationResponse body = caseConclusionCommandService.triggerGeneration(
                caseFileId, fromScratch, oidcUser, OAuthProviderResolver.resolve(principal), principal);
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

    @PatchMapping("/versions/{versionId}/content")
    public ConclusionResponse updateContent(
            @PathVariable UUID caseFileId,
            @PathVariable UUID versionId,
            @RequestBody ContentUpdateRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return caseConclusionCommandService.updateContent(
                caseFileId, versionId, request != null ? request.content() : null,
                oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    /**
     * F-265 / SF-265-01 — régénère le markdown d'UNE section d'une version selon une
     * instruction libre de l'avocat. Synchrone, aucune persistance (le frontend insère
     * dans l'éditeur, l'avocat enregistre via {@code PATCH .../content}).
     */
    @PostMapping("/versions/{versionId}/sections/regenerate")
    public SectionRegenerationResponse regenerateSection(
            @PathVariable UUID caseFileId,
            @PathVariable UUID versionId,
            @RequestBody SectionRegenerationRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return sectionRegenerationService.regenerateSection(
                caseFileId, versionId,
                request != null ? request.sectionMarkdown() : null,
                request != null ? request.instruction() : null,
                oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
