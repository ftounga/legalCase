/**
 * F-255 SF-255-02 — Modèles TypeScript reflétant strictement les DTOs backend
 * `PromoCodeDto` et `PromoCodeCreateRequest` (cf. PR #1253).
 *
 * Source de vérité backend : `backend/src/main/java/fr/ailegalcase/billing/`
 * - `PromoCodeDto.java`
 * - `PromoCodeCreateRequest.java`
 * - `PromoCodeType.java`
 */

/** Type d'un code promo — mapping identique à l'enum Java côté backend. */
export type PromoCodeType = 'TRIAL_EXTENSION' | 'STRIPE_DISCOUNT';

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
  /** Identifiant Stripe (toujours `null` en V1 — réservé à SF-255-04). */
  stripeCouponId: string | null;
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
 * Body envoyé à `POST /api/v1/super-admin/promo-codes`. `valueDays` reste
 * optionnel (uniquement requis si `type = TRIAL_EXTENSION`, vérifié serveur).
 */
export interface PromoCodeCreateRequest {
  code: string;
  type: PromoCodeType;
  valueDays?: number | null;
  partnerLabel: string;
  maxUses: number;
  /** ISO-8601 UTC. */
  expiresAt: string;
}
