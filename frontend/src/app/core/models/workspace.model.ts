/**
 * Plans d'abonnement workspace — alignés avec F-123 (pricing déployé prod).
 * - FREE : essai
 * - SOLO : 1 utilisateur, mono-workspace
 * - TEAM : 3 utilisateurs inclus (cap 6), multi-workspace autorisé
 * - PRO  : 5 utilisateurs inclus (+ extras), multi-workspace autorisé
 */
export type WorkspacePlan = 'FREE' | 'SOLO' | 'TEAM' | 'PRO';

/**
 * Statut workspace.
 * - ACTIVE          : workspace utilisable normalement
 * - PENDING_PAYMENT : créé suite à F-156, en attente du webhook Stripe `customer.subscription.created`
 * - CANCELLED       : abonnement annulé (workspace gelé puis supprimé par cron)
 */
export type WorkspaceStatus = 'ACTIVE' | 'PENDING_PAYMENT' | 'CANCELLED' | string;

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  planCode: string;
  status: WorkspaceStatus;
  expiresAt?: string | null;
  primary?: boolean;
  legalDomain?: string;
  country?: string;
}

/**
 * F-156 SF-156-02 — payload de création d'un workspace supplémentaire.
 * `plan` est obligatoire et limité à TEAM ou PRO côté backend (gate SF-156-01).
 */
export interface WorkspaceCreateRequest {
  name: string;
  plan: 'TEAM' | 'PRO';
}

/**
 * F-156 SF-156-02 — réponse backend à la création (contrat figé SF-156-01).
 * Le frontend redirige le navigateur vers `stripeCheckoutUrl`.
 */
export interface WorkspaceCreateResponse {
  workspaceId: string;
  stripeCheckoutUrl: string;
  status: WorkspaceStatus;
}
