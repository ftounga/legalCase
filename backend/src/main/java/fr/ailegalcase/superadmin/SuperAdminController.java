package fr.ailegalcase.superadmin;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/super-admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final PipelineHealthService pipelineHealthService;
    private final SuperAdminProspectBootstrapService prospectBootstrapService;

    public SuperAdminController(SuperAdminService superAdminService,
                                PipelineHealthService pipelineHealthService,
                                SuperAdminProspectBootstrapService prospectBootstrapService) {
        this.superAdminService = superAdminService;
        this.pipelineHealthService = pipelineHealthService;
        this.prospectBootstrapService = prospectBootstrapService;
    }

    @GetMapping("/workspaces")
    public Page<SuperAdminWorkspaceResponse> listWorkspaces(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return superAdminService.listAllWorkspaces(oidcUser, OAuthProviderResolver.resolve(principal), pageable);
    }

    @GetMapping("/usage")
    public List<SuperAdminUsageResponse> getUsage(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return superAdminService.getUsageByWorkspace(oidcUser, OAuthProviderResolver.resolve(principal));
    }

    @DeleteMapping("/workspaces/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkspace(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PathVariable UUID id) {
        superAdminService.deleteWorkspace(oidcUser, OAuthProviderResolver.resolve(principal), id);
    }

    @GetMapping("/users")
    public Page<SuperAdminUserResponse> listUsers(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PageableDefault(size = 20, sort = "email", direction = Sort.Direction.ASC) Pageable pageable) {
        return superAdminService.listAllUsers(oidcUser, OAuthProviderResolver.resolve(principal), pageable);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PathVariable UUID id) {
        superAdminService.deleteUser(oidcUser, OAuthProviderResolver.resolve(principal), id);
    }

    @GetMapping("/metrics")
    public SuperAdminMetricsResponse getMetrics(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return superAdminService.getMetrics(oidcUser, OAuthProviderResolver.resolve(principal));
    }

    @GetMapping("/pipeline-health")
    public PipelineHealthResponse getPipelineHealth(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return pipelineHealthService.getHealth();
    }

    /**
     * F-147 SF-147-02 : force FAILED sur tous les jobs PENDING/PROCESSING d'un
     * case file. Utile pour débloquer un dossier dont un job est resté coincé
     * (ex: panne API Anthropic sans récupération propre). Retourne le nombre
     * de jobs modifiés.
     */
    @PostMapping("/case-files/{id}/reset-pipeline")
    public ResetPipelineResponse resetPipeline(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PathVariable UUID id) {
        int updated = superAdminService.resetCaseFilePipeline(
                oidcUser, OAuthProviderResolver.resolve(principal), id);
        return new ResetPipelineResponse(id, updated);
    }

    public record ResetPipelineResponse(UUID caseFileId, int jobsForcedFailed) {}

    /**
     * F-251 SF-251-03 : provisionnement super-admin d'un compte prospect
     * (user + AuthAccount LOCAL + workspace + membership + subscription).
     *
     * <p>Remplace la chaîne d'{@code INSERT} SQL de la skill
     * {@code prospect-account-bootstrap} (étape 4) — passe par JPA donc
     * bénéficie du {@code @PrePersist} SF-251-02 sur
     * {@code Subscription.expiresAt} → impossible de recréer le bug
     * Renversez 13/05 via ce chemin.
     */
    @PostMapping("/prospect-bootstrap")
    @ResponseStatus(HttpStatus.CREATED)
    public ProspectBootstrapResponse bootstrapProspect(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @Valid @RequestBody ProspectBootstrapRequest req) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return prospectBootstrapService.bootstrapProspect(req);
    }
}
