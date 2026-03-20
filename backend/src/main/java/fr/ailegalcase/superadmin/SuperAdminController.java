package fr.ailegalcase.superadmin;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

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
}
