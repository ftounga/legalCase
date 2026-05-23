/**
 * F-255 SF-255-02 + SF-255-02b — Modèles TypeScript reflétant strictement les
 * DTOs backend `PromoCodeDto` et `PromoCodeCreateRequest`.
 *
 * Source de vérité backend : `backend/src/main/java/fr/ailegalcase/billing/`
 * - `PromoCodeDto.java`
 * - `PromoCodeCreateRequest.java`
 * - `PromoCodeType.java`
 * - `PromoCodeValueOffType.java`
 * - `PromoCodeDuration.java`
 */

/** Type d'un code promo — mapping identique à l'enum Java côté backend. */
export type PromoCodeType = 'TRIAL_EXTENSION' | 'STRIPE_DISCOUNT';

/**
 * Type de réduction pour les codes STRIPE_DISCOUNT — mapping identique à l'enum
 * Java `PromoCodeValueOffType`.
 *
 * <ul>
 *   <li>`PERCENT` : `valueOffAmount` est un pourcentage 1..100, mappé sur
 *       `coupon.percent_off` Stripe.</li>
 *   <li>`AMOUNT` : `valueOffAmount` est exprimé en centimes (EUR), mappé sur
 *       `coupon.amount_off` + `coupon.currency` Stripe.</li>
 * </ul>
 */
export type PromoCodeValueOffType = 'PERCENT' | 'AMOUNT';

/**
 * Durée d'application d'un code STRIPE_DISCOUNT — mapping identique à l'enum
 * Java `PromoCodeDuration`.
 *
 * <ul>
 *   <li>`ONCE` → Stripe `once` (1ʳᵉ facture uniquement)</li>
 *   <li>`REPEATING_3` → Stripe `repeating` + `duration_in_months=3`</li>
 *   <li>`FOREVER` → Stripe `forever`</li>
 * </ul>
 */
export type PromoCodeDuration = 'ONCE' | 'REPEATING_3' | 'FOREVER';

/**
 * Projection lecture renvoyée par l'API super-admin. `usesCount` est recalculé
 * côté backend depuis `promo_code_redemptions` au moment du listing.
 */
export interface PromoCodeDto {
  id: string;
  code: string;
  type: PromoCodeType;
  /** Nombre de jours d'extension d'essai. `null` quand `type = STRIPE_DISCOUNT`. */
  valueDays: number | null;
  /** Identifiant Stripe Coupon (renseigné quand `type = STRIPE_DISCOUNT`). */
  stripeCouponId: string | null;
  /** Identifiant Stripe PromotionCode (renseigné quand `type = STRIPE_DISCOUNT`). */
  stripePromotionCodeId: string | null;
  /** Type de réduction Stripe (PERCENT/AMOUNT). `null` pour TRIAL_EXTENSION. */
  valueOffType: PromoCodeValueOffType | null;
  /** Valeur de la réduction : pourcentage (1..100) ou centimes (>=100). */
  valueOffAmount: number | null;
  /** ISO-4217 (EUR V1). `null` pour TRIAL_EXTENSION ou PERCENT. */
  currency: string | null;
  /** Durée d'application Stripe. `null` pour TRIAL_EXTENSION. */
  duration: PromoCodeDuration | null;
  partnerLabel: string;
  maxUses: number;
  /** Nombre de redemptions consommées (recompté côté backend). */
  usesCount: number;
  /** ISO-8601 UTC. */
  expiresAt: string;
  active: boolean;
  /** ISO-8601 UTC. */
  createdAt: string;
  createdByUserId: string;
}

/**
 * Body envoyé à `POST /api/v1/super-admin/promo-codes`. Les champs Stripe
 * (`valueOffType`, `valueOffAmount`, `currency`, `duration`) sont requis si
 * `type = STRIPE_DISCOUNT` (vérification serveur). `valueDays` reste optionnel
 * (requis si `type = TRIAL_EXTENSION`, interdit si `STRIPE_DISCOUNT`).
 */
export interface PromoCodeCreateRequest {
  code: string;
  type: PromoCodeType;
  valueDays?: number | null;
  /** Requis si `type = STRIPE_DISCOUNT`. */
  valueOffType?: PromoCodeValueOffType | null;
  /** Requis si `type = STRIPE_DISCOUNT`. 1..100 si PERCENT, 100..100000 (centimes) si AMOUNT. */
  valueOffAmount?: number | null;
  /** Requis si `type = STRIPE_DISCOUNT` ET `valueOffType = AMOUNT`. `EUR` V1. */
  currency?: string | null;
  /** Requis si `type = STRIPE_DISCOUNT`. */
  duration?: PromoCodeDuration | null;
  partnerLabel: string;
  maxUses: number;
  /** ISO-8601 UTC. */
  expiresAt: string;
}
