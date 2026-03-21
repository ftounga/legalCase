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
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService workspaceInvitationService;

    public WorkspaceInvitationController(WorkspaceInvitationService workspaceInvitationService) {
        this.workspaceInvitationService = workspaceInvitationService;
    }

    @PostMapping("/api/v1/workspaces/current/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceInvitationResponse create(@Valid @RequestBody WorkspaceInvitationRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return workspaceInvitationService.createInvitation(request, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/api/v1/workspaces/current/invitations")
    public List<WorkspaceInvitationResponse> list(@AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return workspaceInvitationService.listInvitations(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @DeleteMapping("/api/v1/workspaces/current/invitations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id,
                       @AuthenticationPrincipal OidcUser oidcUser,
                       Principal principal) {
        workspaceInvitationService.revokeInvitation(id, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @PostMapping("/api/v1/workspace/invitations/accept")
    @ResponseStatus(HttpStatus.OK)
    public void accept(@Valid @RequestBody AcceptInvitationRequest request,
                       @AuthenticationPrincipal OidcUser oidcUser,
                       Principal principal) {
        workspaceInvitationService.acceptInvitation(request, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
