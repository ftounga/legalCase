package fr.ailegalcase.stylelearning;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-98 / SF-98-46 — API du corpus de style d'un cabinet (style learning).
 *
 * <p>Contrat figé (parallélisation avec SF-98-48) :
 * <ul>
 *   <li>{@code POST .../style-corpus/documents} (multipart {@code file}) →
 *       {@code 202 {"id":UUID,"status":"PENDING"}} ; {@code 400} ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code GET .../style-corpus/documents} → {@code 200 StyleCorpusDocumentSummary[]}
 *       ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code PATCH .../style-corpus/documents/{id}} body {@code {"active":bool}} →
 *       {@code 200 StyleCorpusDocumentSummary} ; {@code 404} ; {@code 401}.</li>
 *   <li>{@code DELETE .../style-corpus/documents/{id}} → {@code 204} ; {@code 404} ; {@code 401}.</li>
 * </ul>
 * Le controller ne porte aucune logique métier : il délègue à
 * {@link StyleCorpusCommandService}. La {@code style_signature} n'est jamais
 * exposée par l'API (usage interne SF-98-47).</p>
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/style-corpus/documents")
public class StyleCorpusController {

    private final StyleCorpusCommandService styleCorpusCommandService;

    public StyleCorpusController(StyleCorpusCommandService styleCorpusCommandService) {
        this.styleCorpusCommandService = styleCorpusCommandService;
    }

    @PostMapping
    public ResponseEntity<StyleCorpusUploadResponse> upload(
            @PathVariable UUID workspaceId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        StyleCorpusUploadResponse body = styleCorpusCommandService.upload(
                workspaceId, file, oidcUser, OAuthProviderResolver.resolve(principal), principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @GetMapping
    public List<StyleCorpusDocumentSummary> list(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return styleCorpusCommandService.list(
                workspaceId, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @PatchMapping("/{id}")
    public StyleCorpusDocumentSummary updateActive(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id,
            @RequestBody StyleCorpusActiveUpdateRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return styleCorpusCommandService.updateActive(
                workspaceId, id, request != null ? request.active() : null,
                oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        styleCorpusCommandService.delete(
                workspaceId, id, oidcUser, OAuthProviderResolver.resolve(principal), principal);
        return ResponseEntity.noContent().build();
    }
}
