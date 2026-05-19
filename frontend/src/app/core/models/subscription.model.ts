// F-247 / SF-247-02 — état d'abonnement consommé par l'écran billing.
// Contrat figé par SF-247-01 (GET /api/v1/billing/subscription).
export interface SubscriptionState {
  planCode: string;
  status: string;
  cancelAtPeriodEnd: boolean;
  currentPeriodEnd: string | null;
}
