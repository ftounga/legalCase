/**
 * F-240 SF-240-02 — Modèles TypeScript pour l'acceptation des CGU/Privacy.
 */

/** Types de consentement reconnus par le backend (POST /api/v1/consent/accept). */
export type ConsentType = 'SIGNUP_TERMS' | 'PRIVACY_POLICY' | 'DPA' | 'COMMERCIAL_TERMS';

/** Corps de requête pour POST /api/v1/consent/accept. */
export interface ConsentAcceptanceRequest {
  consentTypes: ConsentType[];
  version: string;
}

/** Une acceptation contractuelle persistée — correspond à ConsentAcceptanceResponse.Acceptance (Java). */
export interface Acceptance {
  id: string;
  userId: string;
  consentType: ConsentType;
  version: string;
  acceptedAt: string;
  acceptanceIp: string | null;
  acceptanceUserAgent: string | null;
  workspaceId: string | null;
}

/** Réponse 201 de POST /api/v1/consent/accept. */
export interface ConsentAcceptanceResponse {
  acceptances: Acceptance[];
}
