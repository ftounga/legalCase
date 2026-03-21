package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceMemberService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public WorkspaceMemberService(WorkspaceMemberRepository workspaceMemberRepository,
                                  CurrentUserResolver currentUserResolver) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listMembers(OidcUser oidcUser, String provider, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);
        Workspace workspace = resolveWorkspace(user);

        return workspaceMemberRepository.findByWorkspace_Id(workspace.getId()).stream()
                .map(m -> new WorkspaceMemberResponse(
                        m.getUser().getId(),
                        m.getUser().getEmail(),
                        m.getUser().getFirstName(),
                        m.getUser().getLastName(),
                        m.getMemberRole(),
                        m.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void removeMember(UUID targetUserId, OidcUser oidcUser, String provider, Principal principal) {
        User requestingUser = resolveUser(oidcUser, provider, principal);
        Workspace workspace = resolveWorkspace(requestingUser);

        WorkspaceMember requestingMember = workspaceMemberRepository
                .findByWorkspace_IdAndUser_Id(workspace.getId(), requestingUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        if (!"OWNER".equals(requestingMember.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the OWNER can remove members");
        }

        if (requestingUser.getId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The OWNER cannot remove themselves");
        }

        WorkspaceMember targetMember = workspaceMemberRepository
                .findByWorkspace_IdAndUser_Id(workspace.getId(), targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        workspaceMemberRepository.delete(targetMember);
    }

    private User resolveUser(OidcUser oidcUser, String provider, Principal principal) {
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private Workspace resolveWorkspace(User user) {
        return workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();
    }
}
