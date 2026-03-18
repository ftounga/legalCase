package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceInvitationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REVOKED = "REVOKED";

    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AuthAccountRepository authAccountRepository;
    private final EmailService emailService;

    public WorkspaceInvitationService(WorkspaceInvitationRepository workspaceInvitationRepository,
                                      WorkspaceMemberRepository workspaceMemberRepository,
                                      WorkspaceRepository workspaceRepository,
                                      AuthAccountRepository authAccountRepository,
                                      EmailService emailService) {
        this.workspaceInvitationRepository = workspaceInvitationRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceRepository = workspaceRepository;
        this.authAccountRepository = authAccountRepository;
        this.emailService = emailService;
    }

    @Transactional
    public WorkspaceInvitationResponse createInvitation(WorkspaceInvitationRequest request,
                                                        OidcUser oidcUser, String provider) {
        User requestingUser = resolveUser(oidcUser, provider);
        Workspace workspace = resolveWorkspace(requestingUser);

        WorkspaceMember requestingMember = workspaceMemberRepository
                .findByWorkspace_IdAndUser_Id(workspace.getId(), requestingUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        if (!"OWNER".equals(requestingMember.getMemberRole()) && !"ADMIN".equals(requestingMember.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only OWNER or ADMIN can invite members");
        }

        if (workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(
                workspace.getId(), request.email(), STATUS_PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A pending invitation already exists for this email");
        }

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setWorkspaceId(workspace.getId());
        invitation.setInvitedByUserId(requestingUser.getId());
        invitation.setEmail(request.email());
        invitation.setRole(request.role());
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(STATUS_PENDING);
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        workspaceInvitationRepository.save(invitation);

        emailService.sendInvitation(invitation.getEmail(), workspace.getName(), invitation.getToken());

        return toResponse(invitation);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> listInvitations(OidcUser oidcUser, String provider) {
        User requestingUser = resolveUser(oidcUser, provider);
        Workspace workspace = resolveWorkspace(requestingUser);

        WorkspaceMember requestingMember = workspaceMemberRepository
                .findByWorkspace_IdAndUser_Id(workspace.getId(), requestingUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        if (!"OWNER".equals(requestingMember.getMemberRole()) && !"ADMIN".equals(requestingMember.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only OWNER or ADMIN can list invitations");
        }

        return workspaceInvitationRepository
                .findByWorkspaceIdAndStatus(workspace.getId(), STATUS_PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void revokeInvitation(UUID invitationId, OidcUser oidcUser, String provider) {
        User requestingUser = resolveUser(oidcUser, provider);
        Workspace workspace = resolveWorkspace(requestingUser);

        WorkspaceMember requestingMember = workspaceMemberRepository
                .findByWorkspace_IdAndUser_Id(workspace.getId(), requestingUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        if (!"OWNER".equals(requestingMember.getMemberRole()) && !"ADMIN".equals(requestingMember.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only OWNER or ADMIN can revoke invitations");
        }

        WorkspaceInvitation invitation = workspaceInvitationRepository
                .findByIdAndWorkspaceId(invitationId, workspace.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (!STATUS_PENDING.equals(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING invitations can be revoked");
        }

        invitation.setStatus(STATUS_REVOKED);
        workspaceInvitationRepository.save(invitation);
    }

    @Transactional
    public void acceptInvitation(AcceptInvitationRequest request, OidcUser oidcUser, String provider) {
        User user = resolveUser(oidcUser, provider);

        WorkspaceInvitation invitation = workspaceInvitationRepository
                .findByToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invitation token"));

        if (!STATUS_PENDING.equals(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation is no longer valid");
        }

        if (Instant.now().isAfter(invitation.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation has expired");
        }

        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This invitation was not issued for your email address");
        }

        workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .ifPresent(existing -> {
                    existing.setPrimary(false);
                    workspaceMemberRepository.save(existing);
                });

        Workspace targetWorkspace = workspaceRepository.findById(invitation.getWorkspaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        WorkspaceMember newMember = new WorkspaceMember();
        newMember.setWorkspace(targetWorkspace);
        newMember.setUser(user);
        newMember.setMemberRole(invitation.getRole());
        newMember.setPrimary(true);
        workspaceMemberRepository.save(newMember);

        invitation.setStatus(STATUS_ACCEPTED);
        workspaceInvitationRepository.save(invitation);
    }

    private User resolveUser(OidcUser oidcUser, String provider) {
        return authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();
    }

    private Workspace resolveWorkspace(User user) {
        return workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();
    }

    private WorkspaceInvitationResponse toResponse(WorkspaceInvitation invitation) {
        return new WorkspaceInvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }
}
