# SF-49-01 — Backend top-up tokens (Stripe one-shot)

**Feature parente :** F-49 — Top-up de crédits tokens
**Branche :** feat/SF-49-01-backend-topup

## Objectif

Permettre à un workspace d'acheter un pack de tokens supplémentaires via Stripe (paiement unique, hors abonnement), crédités immédiatement et déduits en priorité sur le quota mensuel du plan.

## Comportement nominal

1. OWNER ou ADMIN appelle `POST /api/v1/stripe/topup-session` avec `packCode` (`TOKENS_1M`, `TOKENS_5M`, `TOKENS_20M`).
2. Le backend crée une Stripe Checkout Session en mode `payment`.
3. L'utilisateur paie → webhook `checkout.session.completed` avec `mode=payment`.
4. Le webhook crée un `CreditPurchase` (workspaceId, tokens, montant, stripeSessionId).
5. `PlanLimitService.isMonthlyTokenBudgetExceeded()` soustrait les crédits restants avant de comparer au budget plan.

**Déduction prioritaire :** crédits d'abord, quota plan ensuite.

## Cas d'erreur

| Cas | Réponse |
|-----|---------|
| Stripe désactivé | 503 |
| packCode inconnu | 400 |
| Workspace introuvable | 404 |
| Erreur Stripe | 502 |
| Webhook signature invalide | 400 |
| Webhook mode ≠ payment | 200 (ignoré) |
| Utilisateur non OWNER/ADMIN | 403 |

## Critères d'acceptation

- [ ] `POST /api/v1/stripe/topup-session {packCode}` → `{checkoutUrl}` pour OWNER/ADMIN
- [ ] 403 si MEMBER
- [ ] Webhook mode=payment → `credit_purchases` créé
- [ ] `isMonthlyTokenBudgetExceeded()` intègre les crédits restants
- [ ] Crédits scoped au workspace
- [ ] Workspace sans crédits → comportement inchangé

## Tables

**Nouvelle table `credit_purchases` (migration 035) :**
- id UUID PK
- workspace_id UUID FK workspaces
- tokens_bought BIGINT NOT NULL
- amount_cents INT NOT NULL
- stripe_session_id VARCHAR(255) UNIQUE
- created_at TIMESTAMPTZ NOT NULL

## Composants

**Nouveaux :** CreditPurchase, CreditPurchaseRepository, CreditPurchaseService, migration 035
**Modifiés :** PlanLimitService, StripeWebhookService, StripeCheckoutController

## Hors périmètre

- UI (SF-49-02)
- Expiration / remboursement des crédits
- Packs configurables dynamiquement
