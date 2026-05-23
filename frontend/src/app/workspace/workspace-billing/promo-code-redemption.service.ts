import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PromoCodeErrorCode, RedeemRequest, RedeemResponse } from './promo-code-redemption.model';

/**
 * F-255 SF-255-03 — service Angular dédié à la redemption d'un code partenaire
 * depuis l'écran /workspace/billing. Encapsule l'appel HTTP et le mapping
 * code applicatif → message FR (consommé par le composant pour le toast).
 *
 * Le contrat backend (figé par SF-255-01, PR #1253) est :
 *   POST /api/v1/workspaces/{workspaceId}/billing/promo-codes/redeem
 *   Body : { code: string }
 *   Réponse 200 : RedeemResponse
 *   Erreurs 4xx : { error, message, code: PromoCodeErrorCode }
 */
@Injectable({ providedIn: 'root' })
export class PromoCodeRedemptionService {
  private readonly http = inject(HttpClient);

  /**
   * POST le code partenaire sur l'endpoint de redemption du workspace ciblé.
   * L'isolation workspace est revérifiée côté backend via la membership.
   */
  redeem(workspaceId: string, code: string): Observable<RedeemResponse> {
    const body: RedeemRequest = { code };
    return this.http.post<RedeemResponse>(
      `/api/v1/workspaces/${workspaceId}/billing/promo-codes/redeem`,
      body
    );
  }
}

/**
 * Mapping figé des codes applicatifs → message utilisateur FR.
 * Toute valeur absente (undefined, code inconnu, erreur réseau) tombe dans le
 * fallback générique. Wording validé par l'étape 0 bis :
 * pas de « gratuit / cadeau / promotion » (terminologie « avantage adhérents »).
 */
export const PROMO_CODE_ERROR_MESSAGES_FR: Record<PromoCodeErrorCode, string> = {
  PROMO_CODE_NOT_FOUND: 'Code partenaire invalide',
  PROMO_CODE_INACTIVE: 'Ce code n\'est plus actif',
  PROMO_CODE_EXPIRED: 'Ce code a expiré',
  PROMO_CODE_EXHAUSTED: 'Ce code a atteint son nombre maximal d\'utilisations',
  WORKSPACE_NOT_TRIALING:
    'Votre essai est terminé — passez au plan payant pour bénéficier d\'autres avantages',
  WORKSPACE_ALREADY_REDEEMED_TRIAL_EXTENSION:
    'Vous avez déjà appliqué un avantage adhérents sur ce workspace',
  PROMO_CODE_TYPE_NOT_SUPPORTED_YET: 'Ce type d\'avantage sera disponible prochainement',
  FORBIDDEN_WORKSPACE: 'Action non autorisée sur ce workspace',
};

/** Fallback affiché si le backend renvoie un code inconnu ou en cas d'erreur réseau. */
export const PROMO_CODE_FALLBACK_ERROR_FR = 'Erreur réseau — veuillez réessayer';

/**
 * Retourne le message FR à afficher à l'utilisateur depuis une erreur HttpErrorResponse.
 * Robuste aux body partiels ou non-structurés.
 */
export function mapPromoCodeError(error: unknown): string {
  const code = (error as { error?: { code?: string } } | undefined)?.error?.code;
  if (code && code in PROMO_CODE_ERROR_MESSAGES_FR) {
    return PROMO_CODE_ERROR_MESSAGES_FR[code as PromoCodeErrorCode];
  }
  return PROMO_CODE_FALLBACK_ERROR_FR;
}
