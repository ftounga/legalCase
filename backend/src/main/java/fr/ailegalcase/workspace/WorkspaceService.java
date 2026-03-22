package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.billing.StripeCustomerService;
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
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AuthAccountRepository authAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StripeCustomerService stripeCustomerService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository workspaceMemberRepository,
                            AuthAccountRepository authAccountRepository,
                            SubscriptionRepository subscriptionRepository,
                            StripeCustomerService stripeCustomerService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.authAccountRepository = authAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.stripeCustomerService = stripeCustomerService;
    }

    @Transactional
    public WorkspaceResponse createWorkspace(OidcUser oidcUser, String provider, String name, String legalDomain, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

        if (workspaceMemberRepository.existsByUser(user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has a workspace");
        }

        Workspace workspace = new Workspace();
        workspace.setName(name.strip());
        workspace.setSlug(UUID.randomUUID().toString());
        workspace.setOwner(user);
        workspace.setLegalDomain(legalDomain);
        workspace.setPlanCode("FREE");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        Instant now = Instant.now();
        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspace.getId());
        subscription.setPlanCode("FREE");
        subscription.setStatus("ACTIVE");
        subscription.setStartedAt(now);
        subscription.setExpiresAt(now.plus(14, ChronoUnit.DAYS));
        subscriptionRepository.save(subscription);

        stripeCustomerService.createCustomer(user.getEmail(), workspace.getId())
                .ifPresent(customerId -> {
                    subscription.setStripeCustomerId(customerId);
                    subscriptionRepository.save(subscription);
                });

        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug(),
                workspace.getPlanCode(), workspace.getStatus(), subscription.getExpiresAt(), true);
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
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("FREE");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        Instant now = Instant.now();
        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspace.getId());
        subscription.setPlanCode("FREE");
        subscription.setStatus("ACTIVE");
        subscription.setStartedAt(now);
        subscription.setExpiresAt(now.plus(14, ChronoUnit.DAYS));
        subscriptionRepository.save(subscription);

        stripeCustomerService.createCustomer(user.getEmail(), workspace.getId())
                .ifPresent(customerId -> {
                    subscription.setStripeCustomerId(customerId);
                    subscriptionRepository.save(subscription);
                });
    }

    private User resolveUser(OidcUser oidcUser, String provider, Principal principal) {
        if (oidcUser != null) {
            return authAccountRepository
                    .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                    .getUser();
        }
        // Auth locale : principal.getName() = email
        return authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide"))
                .getUser();
    }

    @Transactional
    public WorkspaceResponse getCurrentWorkspace(OidcUser oidcUser, String provider, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

        WorkspaceMember member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseGet(() -> {
                    List<WorkspaceMember> members = workspaceMemberRepository.findByUser(user);
                    if (members.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
                    WorkspaceMember fallback = members.get(0);
                    fallback.setPrimary(true);
                    return workspaceMemberRepository.save(fallback);
                });
        Workspace workspace = member.getWorkspace();

        Instant expiresAt = subscriptionRepository.findByWorkspaceId(workspace.getId())
                .map(Subscription::getExpiresAt)
                .orElse(null);

        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug(),
                workspace.getPlanCode(), workspace.getStatus(), expiresAt, true);
    }

    @Transactional(readOnly = true)
    public java.util.List<WorkspaceResponse> listUserWorkspaces(OidcUser oidcUser, String provider, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

        return workspaceMemberRepository.findByUser(user).stream()
                .map(member -> {
                    Workspace ws = member.getWorkspace();
                    Instant expiresAt = subscriptionRepository.findByWorkspaceId(ws.getId())
                            .map(Subscription::getExpiresAt).orElse(null);
                    return new WorkspaceResponse(ws.getId(), ws.getName(), ws.getSlug(),
                            ws.getPlanCode(), ws.getStatus(), expiresAt, member.isPrimary());
                })
                .toList();
    }

    @Transactional
    public WorkspaceResponse switchWorkspace(OidcUser oidcUser, String provider, UUID targetWorkspaceId, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

        WorkspaceMember target = workspaceMemberRepository
                .findByWorkspace_IdAndUser_Id(targetWorkspaceId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this workspace"));

        workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .ifPresent(current -> {
                    current.setPrimary(false);
                    workspaceMemberRepository.save(current);
                });

        target.setPrimary(true);
        workspaceMemberRepository.save(target);

        Workspace ws = target.getWorkspace();
        Instant expiresAt = subscriptionRepository.findByWorkspaceId(ws.getId())
                .map(Subscription::getExpiresAt).orElse(null);

        return new WorkspaceResponse(ws.getId(), ws.getName(), ws.getSlug(),
                ws.getPlanCode(), ws.getStatus(), expiresAt, true);
    }
}
