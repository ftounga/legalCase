package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.billing.StripeSeatService;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.PaymentRequiredCode;
import fr.ailegalcase.shared.PaymentRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
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
    private final CurrentUserResolver currentUserResolver;
    private final EmailService emailService;
    private final PlanLimitService planLimitService;
    private final StripeSeatService stripeSeatService;
    private final WorkspaceAccessGuard workspaceAccessGuard;

    public WorkspaceInvitationService(WorkspaceInvitationRepository workspaceInvitationRepository,
                                      WorkspaceMemberRepository workspaceMemberRepository,
                                      WorkspaceRepository workspaceRepository,
                                      CurrentUserResolver currentUserResolver,
                                      EmailService emailService,
                                      PlanLimitService planLimitService,
                                      StripeSeatService stripeSeatService,
                                      WorkspaceAccessGuard workspaceAccessGuard) {
        this.workspaceInvitationRepository = workspaceInvitationRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceRepository = workspaceRepository;
        this.currentUserResolver = currentUserResolver;
        this.emailService = emailService;
        this.planLimitService = planLimitService;
        this.stripeSeatService = stripeSeatService;
        this.workspaceAccessGuard = workspaceAccessGuard;
    }

    @Transactional
    public WorkspaceInvitationResponse createInvitation(WorkspaceInvitationRequest request,
                                                        OidcUser oidcUser, String provider, Principal principal) {
        User requestingUser = resolveUser(oidcUser, provider, principal);
        Workspace workspace = resolveWorkspace(requestingUser);

        // SF-156-01 (CA8) : refuser l'invitation si workspace PENDING_PAYMENT
        // ou CANCELLED — invariant SF-156-00 §1.
        workspaceAccessGuard.requireActive(workspace);

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

        // SF-123-02 : gate seat — seats actuels + pendantes + 1 ≤ max du plan
        int currentMembers = workspaceMemberRepository.findByWorkspace_Id(workspace.getId()).size();
        int currentPending = workspaceInvitationRepository
                .findByWorkspaceIdAndStatus(workspace.getId(), STATUS_PENDING).size();
        int maxSeats = planLimitService.getMaxSeatsForWorkspace(workspace.getId());
        if (currentMembers + currentPending + 1 > maxSeats) {
            throw new PaymentRequiredException(PaymentRequiredCode.SEAT_LIMIT_EXCEEDED,
                    seatLimitMessage(workspace.getId()));
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
    public List<WorkspaceInvitationResponse> listInvitations(OidcUser oidcUser, String provider, Principal principal) {
        User requestingUser = resolveUser(oidcUser, provider, principal);
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
    public void revokeInvitation(UUID invitationId, OidcUser oidcUser, String provider, Principal principal) {
        User requestingUser = resolveUser(oidcUser, provider, principal);
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
    public void acceptInvitation(AcceptInvitationRequest request, OidcUser oidcUser, String provider, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

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

        // SF-123-02 : sync Stripe quantity après ajout membre.
        // Transactionnel : si Stripe refuse (402), rollback → membre non persisté.
        stripeSeatService.syncSeatCount(targetWorkspace.getId());
    }

    private String seatLimitMessage(java.util.UUID workspaceId) {
        String plan = planLimitService.getPlanCodeForWorkspace(workspaceId);
        return switch (plan) {
            case "SOLO" -> "Passez à TEAM pour inviter un collaborateur (jusqu'à 6 utilisateurs).";
            case "TEAM" -> "Plan TEAM limité à 6 utilisateurs. Passez à PRO pour en ajouter davantage.";
            case "FREE" -> "Le plan FREE ne permet pas d'inviter de collaborateur.";
            default     -> "Limite d'utilisateurs atteinte pour votre plan.";
        };
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
