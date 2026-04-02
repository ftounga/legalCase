package fr.ailegalcase.superadmin;

import fr.ailegalcase.shared.OAuthProviderResolver;
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

    public SuperAdminController(SuperAdminService superAdminService,
                                PipelineHealthService pipelineHealthService) {
        this.superAdminService = superAdminService;
        this.pipelineHealthService = pipelineHealthService;
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
}
