/**
 * Payload envoyé au backend pour signaler une erreur runtime côté navigateur.
 *
 * SF-INFRA-09 — remplace Sentry par une chaîne CloudWatch native.
 */
export interface ClientErrorPayload {
  /** Description courte de l'erreur. Tronqué à 500 chars côté frontend. */
  message: string;
  /** Stack trace JS. Tronquée à 4000 chars. */
  stack?: string;
  /** URL où l'erreur est survenue (location.href). Tronquée à 500. */
  url?: string;
  /** navigator.userAgent. Tronqué à 200. */
  userAgent?: string;
  /** ISO-8601 timestamp côté client (si absent, backend remplit). */
  timestamp?: string;
  /** Version applicative si dispo dans environment. */
  appVersion?: string;
}
