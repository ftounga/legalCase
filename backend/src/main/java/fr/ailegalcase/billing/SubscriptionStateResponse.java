package fr.ailegalcase.billing;

import java.time.Instant;

/**
 * SF-247-01 : état de résiliation de l'abonnement du workspace courant.
 * Contrat de réponse figé pour la parallélisation SF-247-02 (frontend) :
 * cancel / resume / subscription renvoient tous ce record.
 *
 * @param planCode          plan courant (FREE, SOLO, TEAM, PRO)
 * @param status            statut de l'abonnement local (ACTIVE, ...)
 * @param cancelAtPeriodEnd true si une résiliation est programmée en fin de période
 * @param currentPeriodEnd  date de fin de la période Stripe en cours (ISO-8601), null si FREE
 */
public record SubscriptionStateResponse(
        String planCode,
        String status,
        boolean cancelAtPeriodEnd,
        Instant currentPeriodEnd
) {
    static SubscriptionStateResponse from(Subscription subscription) {
        return new SubscriptionStateResponse(
                subscription.getPlanCode(),
                subscription.getStatus(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getCurrentPeriodEnd());
    }
}
