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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    public void createDefaultWorkspace(User user) {
        if (workspaceMemberRepository.existsByUser(user)) {
            return;
        }

        Workspace workspace = new Workspace();
        workspace.setName(user.getEmail());
        workspace.setSlug(UUID.randomUUID().toString());
        workspace.setOwner(user);
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

    @Transactional(readOnly = true)
    public WorkspaceResponse getCurrentWorkspace(OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getUser();

        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug(),
                workspace.getPlanCode(), workspace.getStatus());
    }
}
