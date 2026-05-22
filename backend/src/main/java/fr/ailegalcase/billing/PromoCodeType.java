package fr.ailegalcase.billing;

/**
 * F-255 SF-255-01 — type de code promo.
 *
 * <ul>
 *   <li>{@link #TRIAL_EXTENSION} — extension d'essai locale, sans Stripe.
 *       Pleinement opérationnel en SF-01 : la redemption étend
 *       {@code subscriptions.expires_at} de {@code valueDays}.</li>
 *   <li>{@link #STRIPE_DISCOUNT} — réduction abonnement via Stripe Coupons.
 *       Stockable en table mais redemption refusée en SF-01 (409
 *       PROMO_CODE_TYPE_NOT_SUPPORTED_YET). Sera activé par SF-255-04.</li>
 * </ul>
 */
public enum PromoCodeType {
    TRIAL_EXTENSION,
    STRIPE_DISCOUNT
}
