package fr.ailegalcase.billing;

/**
 * F-255 SF-255-04 — type de réduction porté par un {@link PromoCode} de type
 * {@link PromoCodeType#STRIPE_DISCOUNT}.
 *
 * <p>{@link #PERCENT} : {@code value_off_amount} est un pourcentage 1..100 ;
 * mappé sur {@code coupon.percent_off} côté Stripe (BigDecimal).
 *
 * <p>{@link #AMOUNT} : {@code value_off_amount} est exprimé en plus petite
 * unité monétaire (centimes pour EUR) ; mappé sur {@code coupon.amount_off}
 * + {@code coupon.currency} côté Stripe.
 */
public enum PromoCodeValueOffType {
    PERCENT,
    AMOUNT
}
