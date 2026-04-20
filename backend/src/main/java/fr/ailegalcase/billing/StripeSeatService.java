package fr.ailegalcase.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.SubscriptionItem;
import com.stripe.param.SubscriptionItemUpdateParams;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * SF-123-02 : synchronise le nombre de seats côté Stripe et côté DB.
 *
 * Appelé après acceptInvitation (création d'un membre) et removeMember
 * (suppression). Pour plans TEAM/PRO avec stripeSubscriptionId, pousse la
 * nouvelle quantity sur l'item V2. Pour FREE/SOLO (1 seat max) et pour
 * les workspaces sans subscription Stripe active, met juste à jour le
 * compteur local sans appel externe.
 *
 * Les incréments (+59 € TEAM, +79 € PRO) sont gérés côté Stripe via
 * tiered pricing : tranche 1 = seats inclus à 0 €, tranche 2 = seats
 * supp au tarif unitaire. Le backend ne fait que pousser la quantity.
 */
@Service
public class StripeSeatService {

    private static final Logger log = LoggerFactory.getLogger(StripeSeatService.class);

    private final boolean stripeEnabled;
    private final String secretKey;
    private final SubscriptionRepository subscriptionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public StripeSeatService(
            @Value("${app.stripe.enabled:false}") boolean stripeEnabled,
            @Value("${app.stripe.secret-key:}") String secretKey,
            SubscriptionRepository subscriptionRepository,
            WorkspaceMemberRepository workspaceMemberRepository) {
        this.stripeEnabled = stripeEnabled;
        this.secretKey = secretKey;
        this.subscriptionRepository = subscriptionRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Recalcule le nombre de membres actifs et le synchronise.
     * Pour TEAM/PRO avec stripeSubId configuré, pousse Subscription.update
     * avec proration native. Pour FREE/SOLO ou absence de stripeSubId,
     * no-op côté Stripe — on met juste à jour seatCount DB.
     */
    @Transactional
    public void syncSeatCount(UUID workspaceId) {
        Subscription sub = subscriptionRepository.findByWorkspaceId(workspaceId).orElse(null);
        if (sub == null) {
            log.debug("No subscription for workspace {} — skipping seat sync", workspaceId);
            return;
        }

        int memberCount = workspaceMemberRepository.findByWorkspace_Id(workspaceId).size();
        int newSeatCount = Math.max(1, memberCount);

        String plan = sub.getPlanCode();
        boolean billable = ("TEAM".equals(plan) || "PRO".equals(plan))
                && sub.getStripeSubscriptionId() != null
                && !sub.getStripeSubscriptionId().isBlank();

        if (billable && stripeEnabled) {
            try {
                Stripe.apiKey = secretKey;
                com.stripe.model.Subscription stripeSub =
                        com.stripe.model.Subscription.retrieve(sub.getStripeSubscriptionId());
                if (stripeSub.getItems() == null || stripeSub.getItems().getData().isEmpty()) {
                    log.warn("Stripe subscription {} has no items — skipping quantity update",
                            sub.getStripeSubscriptionId());
                } else {
                    SubscriptionItem item = stripeSub.getItems().getData().get(0);
                    SubscriptionItemUpdateParams params = SubscriptionItemUpdateParams.builder()
                            .setQuantity((long) newSeatCount)
                            .setProrationBehavior(SubscriptionItemUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                            .build();
                    item.update(params);
                    log.info("Stripe quantity synced to {} for subscription {} (workspace {})",
                            newSeatCount, sub.getStripeSubscriptionId(), workspaceId);
                }
            } catch (StripeException e) {
                log.error("Stripe update failed for workspace {} (sub {}): {}",
                        workspaceId, sub.getStripeSubscriptionId(), e.getMessage());
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                        "Paiement refusé — mettez à jour votre carte avant d'ajouter un membre", e);
            }
        }

        sub.setSeatCount(newSeatCount);
        subscriptionRepository.save(sub);
    }
}
