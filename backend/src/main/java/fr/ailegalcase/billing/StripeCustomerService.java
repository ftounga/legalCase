package fr.ailegalcase.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class StripeCustomerService {

    private static final Logger log = LoggerFactory.getLogger(StripeCustomerService.class);

    private final boolean stripeEnabled;
    private final String secretKey;

    public StripeCustomerService(
            @Value("${app.stripe.enabled:false}") boolean stripeEnabled,
            @Value("${app.stripe.secret-key:}") String secretKey) {
        this.stripeEnabled = stripeEnabled;
        this.secretKey = secretKey;
    }

    public Optional<String> createCustomer(String email, UUID workspaceId) {
        if (!stripeEnabled) {
            log.debug("Stripe disabled — skipping customer creation for workspace {}", workspaceId);
            return Optional.empty();
        }
        try {
            Stripe.apiKey = secretKey;
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("workspace_id", workspaceId.toString())
                    .build();
            Customer customer = Customer.create(params);
            return Optional.of(customer.getId());
        } catch (StripeException e) {
            log.warn("Failed to create Stripe customer for workspace {} ({}): {}", workspaceId, email, e.getMessage());
            return Optional.empty();
        }
    }

    public void cancelSubscription(String stripeSubscriptionId) {
        if (!stripeEnabled) {
            log.debug("Stripe disabled — skipping subscription cancellation for {}", stripeSubscriptionId);
            return;
        }
        try {
            Stripe.apiKey = secretKey;
            Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
            subscription.cancel();
        } catch (StripeException e) {
            log.warn("Failed to cancel Stripe subscription {}: {}", stripeSubscriptionId, e.getMessage());
            throw new RuntimeException("Stripe cancellation failed", e);
        }
    }

    /**
     * SF-247-01 : programme la résiliation de l'abonnement pour la fin de la
     * période de facturation en cours (cancel_at_period_end = true). Contrairement
     * à {@link #cancelSubscription(String)} (annulation immédiate, F-25 super-admin),
     * l'abonnement reste actif jusqu'à l'échéance.
     *
     * @return la subscription Stripe mise à jour (pour lire current_period_end).
     * @throws RuntimeException si l'appel Stripe échoue.
     */
    public Subscription scheduleCancellation(String stripeSubscriptionId) {
        return updateCancelAtPeriodEnd(stripeSubscriptionId, true);
    }

    /**
     * SF-247-01 : annule une résiliation programmée (cancel_at_period_end = false),
     * l'abonnement reprend son cycle normal.
     *
     * @return la subscription Stripe mise à jour (pour lire current_period_end).
     * @throws RuntimeException si l'appel Stripe échoue.
     */
    public Subscription resumeSubscription(String stripeSubscriptionId) {
        return updateCancelAtPeriodEnd(stripeSubscriptionId, false);
    }

    private Subscription updateCancelAtPeriodEnd(String stripeSubscriptionId, boolean cancelAtPeriodEnd) {
        if (!stripeEnabled) {
            log.debug("Stripe disabled — skipping cancel_at_period_end={} for {}",
                    cancelAtPeriodEnd, stripeSubscriptionId);
            return null;
        }
        try {
            Stripe.apiKey = secretKey;
            Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(cancelAtPeriodEnd)
                    .build();
            return subscription.update(params);
        } catch (StripeException e) {
            log.warn("Failed to set cancel_at_period_end={} on Stripe subscription {}: {}",
                    cancelAtPeriodEnd, stripeSubscriptionId, e.getMessage());
            throw new RuntimeException("Stripe cancellation update failed", e);
        }
    }
}
