package fr.ailegalcase.superadmin;

import fr.ailegalcase.shared.OAuthProviderResolver;
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

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @GetMapping("/workspaces")
    public List<SuperAdminWorkspaceResponse> listWorkspaces(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return superAdminService.listAllWorkspaces(oidcUser, OAuthProviderResolver.resolve(principal));
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
    public List<SuperAdminUserResponse> listUsers(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return superAdminService.listAllUsers(oidcUser, OAuthProviderResolver.resolve(principal));
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PathVariable UUID id) {
        superAdminService.deleteUser(oidcUser, OAuthProviderResolver.resolve(principal), id);
    }
}
