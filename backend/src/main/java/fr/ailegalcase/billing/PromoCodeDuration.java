package fr.ailegalcase.billing;

/**
 * F-255 SF-255-04 — durée d'application d'un {@link PromoCode} de type
 * {@link PromoCodeType#STRIPE_DISCOUNT}, mappée sur la
 * {@code coupon.duration} Stripe.
 *
 * <ul>
 *   <li>{@link #ONCE} → Stripe {@code once} : appliqué uniquement à la
 *       première facture.</li>
 *   <li>{@link #REPEATING_3} → Stripe {@code repeating} +
 *       {@code duration_in_months=3} : appliqué pendant 3 cycles.</li>
 *   <li>{@link #FOREVER} → Stripe {@code forever} : appliqué à toutes les
 *       factures de l'abonnement.</li>
 * </ul>
 */
public enum PromoCodeDuration {
    ONCE,
    REPEATING_3,
    FOREVER
}
