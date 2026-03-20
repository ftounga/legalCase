package fr.ailegalcase.superadmin;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class SuperAdminService {

    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SuperAdminService(AuthAccountRepository authAccountRepository,
                             WorkspaceRepository workspaceRepository,
                             WorkspaceMemberRepository workspaceMemberRepository,
                             SubscriptionRepository subscriptionRepository) {
        this.authAccountRepository = authAccountRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<SuperAdminWorkspaceResponse> listAllWorkspaces(OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        if (!user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }

        return workspaceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private SuperAdminWorkspaceResponse toResponse(Workspace ws) {
        Instant expiresAt = subscriptionRepository.findByWorkspaceId(ws.getId())
                .map(Subscription::getExpiresAt)
                .orElse(null);
        long memberCount = workspaceMemberRepository.findByWorkspace_Id(ws.getId()).size();
        return new SuperAdminWorkspaceResponse(
                ws.getId(), ws.getName(), ws.getSlug(),
                ws.getPlanCode(), ws.getStatus(), expiresAt,
                memberCount, ws.getCreatedAt()
        );
    }
}
