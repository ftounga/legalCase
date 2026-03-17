package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AuthAccountRepository authAccountRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository workspaceMemberRepository,
                            AuthAccountRepository authAccountRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.authAccountRepository = authAccountRepository;
    }

    @Transactional
    public void createDefaultWorkspace(User user) {
        if (workspaceMemberRepository.existsByUser(user)) {
            return;
        }

        Workspace workspace = new Workspace();
        workspace.setName(user.getEmail());
        workspace.setSlug(UUID.randomUUID().toString());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        workspaceMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getCurrentWorkspace(OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getUser();

        Workspace workspace = workspaceMemberRepository
                .findFirstByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug(),
                workspace.getPlanCode(), workspace.getStatus());
    }
}
