package fr.ailegalcase.workspace;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/current")
    public WorkspaceResponse current(@AuthenticationPrincipal OidcUser oidcUser, Principal principal) {
        return workspaceService.getCurrentWorkspace(oidcUser, OAuthProviderResolver.resolve(principal));
    }

    @GetMapping
    public List<WorkspaceResponse> list(@AuthenticationPrincipal OidcUser oidcUser, Principal principal) {
        return workspaceService.listUserWorkspaces(oidcUser, OAuthProviderResolver.resolve(principal).toUpperCase());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(@AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal,
                                    @Valid @RequestBody CreateWorkspaceRequest request) {
        return workspaceService.createWorkspace(oidcUser, OAuthProviderResolver.resolve(principal).toUpperCase(), request.name());
    }

    @PostMapping("/{id}/switch")
    public WorkspaceResponse switchWorkspace(@AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal,
                                             @PathVariable UUID id) {
        return workspaceService.switchWorkspace(oidcUser, OAuthProviderResolver.resolve(principal).toUpperCase(), id);
    }
}
