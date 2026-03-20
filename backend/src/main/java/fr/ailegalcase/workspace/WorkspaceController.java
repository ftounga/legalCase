package fr.ailegalcase.workspace;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(@AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal,
                                    @Valid @RequestBody CreateWorkspaceRequest request) {
        return workspaceService.createWorkspace(oidcUser, OAuthProviderResolver.resolve(principal).toUpperCase(), request.name());
    }
}
