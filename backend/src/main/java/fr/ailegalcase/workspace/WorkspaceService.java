package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.billing.StripeCustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AuthAccountRepository authAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StripeCustomerService stripeCustomerService;
    private final EmailService emailService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository workspaceMemberRepository,
                            AuthAccountRepository authAccountRepository,
                            SubscriptionRepository subscriptionRepository,
                            StripeCustomerService stripeCustomerService,
                            EmailService emailService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.authAccountRepository = authAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.stripeCustomerService = stripeCustomerService;
        this.emailService = emailService;
    }

    @Transactional
    public WorkspaceResponse createWorkspace(OidcUser oidcUser, String provider, String name, String legalDomain, String country, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

        // F-154 : un même user peut désormais avoir plusieurs workspaces (onboarding,
        // puis switcher toolbar). Le 1er workspace devient primary par défaut ; les
        // suivants sont créés non-primary (c'est `switchWorkspace` qui pilote
        // l'unicité du primary ensuite).
        boolean isFirstWorkspace = !workspaceMemberRepository.existsByUser(user);

        Workspace workspace = new Workspace();
        workspace.setName(name.strip().toUpperCase());
        workspace.setSlug(UUID.randomUUID().toString());
        workspace.setOwner(user);
        workspace.setLegalDomain(legalDomain);
        workspace.setCountry(country);
        workspace.setPlanCode("FREE");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(isFirstWorkspace);
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

        // F-154 : l'email "bienvenue onboarding" ne part qu'à la création du 1er
        // workspace (inscription). Un user qui ajoute un 2e/3e workspace depuis la
        // toolbar ne reçoit pas de mail pour éviter le spam.
        if (isFirstWorkspace) {
            try {
                emailService.sendOnboardingWelcome(user);
            } catch (Exception e) {
                log.warn("Failed to send onboarding welcome to {} — {}", user.getEmail(), e.getMessage());
            }
        }

        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug(),
                workspace.getPlanCode(), workspace.getStatus(), subscription.getExpiresAt(), isFirstWorkspace,
                workspace.getLegalDomain(), workspace.getCountry());
    }

    @Transactional
    public void createDefaultWorkspace(User user) {
        if (workspaceMemberRepository.existsByUser(user)) {
            return;
        }

        Workspace workspace = new Workspace();
        workspace.setName(user.getEmail().toUpperCase());
        workspace.setSlug(UUID.randomUUID().toString());
        workspace.setOwner(user);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setCountry("FRANCE");
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

        Subscription sub = subscriptionRepository.findByWorkspaceId(workspace.getId()).orElse(null);
        String planCode = sub != null ? sub.getPlanCode() : workspace.getPlanCode();
        Instant expiresAt = sub != null ? sub.getExpiresAt() : null;

        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug(),
                planCode, workspace.getStatus(), expiresAt, true,
                workspace.getLegalDomain(), workspace.getCountry());
    }

    @Transactional(readOnly = true)
    public java.util.List<WorkspaceResponse> listUserWorkspaces(OidcUser oidcUser, String provider, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

        return workspaceMemberRepository.findByUser(user).stream()
                .map(member -> {
                    Workspace ws = member.getWorkspace();
                    Subscription sub = subscriptionRepository.findByWorkspaceId(ws.getId()).orElse(null);
                    String planCode = sub != null ? sub.getPlanCode() : ws.getPlanCode();
                    Instant expiresAt = sub != null ? sub.getExpiresAt() : null;
                    return new WorkspaceResponse(ws.getId(), ws.getName(), ws.getSlug(),
                            planCode, ws.getStatus(), expiresAt, member.isPrimary(),
                            ws.getLegalDomain(), ws.getCountry());
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
                ws.getPlanCode(), ws.getStatus(), expiresAt, true,
                ws.getLegalDomain(), ws.getCountry());
    }
}
