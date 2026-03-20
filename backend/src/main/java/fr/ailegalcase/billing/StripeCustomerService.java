package fr.ailegalcase.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
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
}
