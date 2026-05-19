package fr.ailegalcase.billing;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-247-01 : orchestration de la résiliation d'abonnement en self-service.
 *
 * <p>Sémantique unique : résiliation en fin de période de facturation
 * (cancel_at_period_end). L'annulation immédiate reste exclusivement
 * super-admin (F-25, {@link StripeCustomerService#cancelSubscription}).
 */
@Service
public class BillingCancellationService {

    private static final Logger log = LoggerFactory.getLogger(BillingCancellationService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final StripeCustomerService stripeCustomerService;

    public BillingCancellationService(SubscriptionRepository subscriptionRepository,
                                      WorkspaceMemberRepository workspaceMemberRepository,
                                      CurrentUserResolver currentUserResolver,
                                      StripeCustomerService stripeCustomerService) {
        this.subscriptionRepository = subscriptionRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.stripeCustomerService = stripeCustomerService;
    }

    /**
     * Programme la résiliation de l'abonnement du workspace courant pour la fin
     * de la période de facturation. Réservé à l'OWNER.
     */
    @Transactional
    public SubscriptionStateResponse cancel(OidcUser oidcUser, String provider, Principal principal) {
        Subscription subscription = resolveOwnedSubscription(oidcUser, provider, principal);

        if ("FREE".equals(subscription.getPlanCode()) || subscription.getStripeSubscriptionId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No paid subscription to cancel");
        }
        if (subscription.isCancelAtPeriodEnd()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cancellation is already scheduled");
        }

        com.stripe.model.Subscription stripeSub;
        try {
            stripeSub = stripeCustomerService.scheduleCancellation(subscription.getStripeSubscriptionId());
        } catch (RuntimeException e) {
            log.error("Stripe scheduleCancellation failed for subscription {}: {}",
                    subscription.getStripeSubscriptionId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Subscription cancellation failed, please retry");
        }
        // Stripe désactivé : scheduleCancellation est un no-op (retourne null).
        // L'état local ne doit pas être modifié — la résiliation n'est pas effective.
        if (stripeSub == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Billing is not available, cancellation cannot be processed");
        }

        subscription.setCancelAtPeriodEnd(true);
        if (stripeSub.getCurrentPeriodEnd() != null) {
            subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()));
        }
        subscriptionRepository.save(subscription);
        log.info("Cancellation scheduled for workspace {} (subscription {})",
                subscription.getWorkspaceId(), subscription.getStripeSubscriptionId());
        return SubscriptionStateResponse.from(subscription);
    }

    /**
     * Annule une résiliation programmée sur l'abonnement du workspace courant.
     * Réservé à l'OWNER.
     */
    @Transactional
    public SubscriptionStateResponse resume(OidcUser oidcUser, String provider, Principal principal) {
        Subscription subscription = resolveOwnedSubscription(oidcUser, provider, principal);

        if ("FREE".equals(subscription.getPlanCode()) || subscription.getStripeSubscriptionId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No paid subscription to resume");
        }
        if (!subscription.isCancelAtPeriodEnd()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No scheduled cancellation");
        }

        com.stripe.model.Subscription stripeSub;
        try {
            stripeSub = stripeCustomerService.resumeSubscription(subscription.getStripeSubscriptionId());
        } catch (RuntimeException e) {
            log.error("Stripe resumeSubscription failed for subscription {}: {}",
                    subscription.getStripeSubscriptionId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Subscription cancellation failed, please retry");
        }
        // Stripe désactivé : resumeSubscription est un no-op (retourne null).
        // L'état local ne doit pas être modifié.
        if (stripeSub == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Billing is not available, cancellation cannot be processed");
        }

        subscription.setCancelAtPeriodEnd(false);
        if (stripeSub.getCurrentPeriodEnd() != null) {
            subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()));
        }
        subscriptionRepository.save(subscription);
        log.info("Scheduled cancellation resumed for workspace {} (subscription {})",
                subscription.getWorkspaceId(), subscription.getStripeSubscriptionId());
        return SubscriptionStateResponse.from(subscription);
    }

    /**
     * Renvoie l'état de résiliation de l'abonnement du workspace courant.
     * Accessible à tout membre du workspace (lecture seule).
     */
    @Transactional(readOnly = true)
    public SubscriptionStateResponse getState(OidcUser oidcUser, String provider, Principal principal) {
        WorkspaceMember member = resolveCurrentMember(oidcUser, provider, principal);
        Subscription subscription = loadSubscription(member.getWorkspace().getId());
        return SubscriptionStateResponse.from(subscription);
    }

    /**
     * Résout l'abonnement du workspace courant en exigeant le rôle OWNER.
     */
    private Subscription resolveOwnedSubscription(OidcUser oidcUser, String provider, Principal principal) {
        WorkspaceMember member = resolveCurrentMember(oidcUser, provider, principal);
        if (!"OWNER".equals(member.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the workspace owner can manage the subscription");
        }
        return loadSubscription(member.getWorkspace().getId());
    }

    private WorkspaceMember resolveCurrentMember(OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        return workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
    }

    private Subscription loadSubscription(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Subscription not found"));
    }
}
