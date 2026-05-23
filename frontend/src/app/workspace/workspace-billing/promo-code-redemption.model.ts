/**
 * F-255 SF-255-03 — contrat TypeScript figé par le backend SF-255-01 (PR #1253).
 *
 * Endpoint : POST /api/v1/workspaces/{workspaceId}/billing/promo-codes/redeem
 * Body de requête : {@link RedeemRequest}
 * Réponse 200 : {@link RedeemResponse}
 * Erreurs métier : {@link PromoCodeErrorCode} (4xx, body { error, message, code }).
 */

/** Body envoyé à l'endpoint de redemption. */
export interface RedeemRequest {
  /** Code partenaire saisi par l'utilisateur (normalisé uppercase + trim côté front). */
  code: string;
}

/** Réponse 200 d'une redemption TRIAL_EXTENSION réussie. */
export interface RedeemResponse {
  /** Nouvelle date d'expiration de l'essai après extension (ISO 8601). */
  newExpiresAt: string;
  /** Nombre de jours effectivement appliqués. */
  addedDays: number;
  /** Étiquette partenaire (ex. « Adhérents ACE »), affichée en confirmation. */
  partnerLabel: string;
}

/**
 * Codes d'erreur machine-readable retournés par le backend dans le champ `code`
 * du body d'erreur (cf. {@code fr.ailegalcase.billing.PromoCodeErrorCode}).
 * Limité aux codes susceptibles d'être vus depuis l'endpoint user de redemption.
 */
export type PromoCodeErrorCode =
  | 'PROMO_CODE_NOT_FOUND'
  | 'PROMO_CODE_INACTIVE'
  | 'PROMO_CODE_EXPIRED'
  | 'PROMO_CODE_EXHAUSTED'
  | 'WORKSPACE_NOT_TRIALING'
  | 'WORKSPACE_ALREADY_REDEEMED_TRIAL_EXTENSION'
  | 'PROMO_CODE_TYPE_NOT_SUPPORTED_YET'
  | 'FORBIDDEN_WORKSPACE';
