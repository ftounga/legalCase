package fr.ailegalcase.workspace;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/current/members")
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;

    public WorkspaceMemberController(WorkspaceMemberService workspaceMemberService) {
        this.workspaceMemberService = workspaceMemberService;
    }

    @GetMapping
    public List<WorkspaceMemberResponse> list(@AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return workspaceMemberService.listMembers(oidcUser, OAuthProviderResolver.resolve(principal));
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID userId,
                       @AuthenticationPrincipal OidcUser oidcUser,
                       Principal principal) {
        workspaceMemberService.removeMember(userId, oidcUser, OAuthProviderResolver.resolve(principal));
    }
}
