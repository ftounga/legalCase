# Mini-spec — SF-19-01 — Infrastructure Stripe : SDK, colonnes, Customer à la création workspace

## Identifiant
`SF-19-01`

## Feature parente
`F-19` — Intégration paiement Stripe

## Statut
`done`

## Date de création
2026-03-18

## Branche Git
`feat/SF-19-01-stripe-infra`

---

## Objectif
Mettre en place les fondations Stripe : dépendance SDK Java, ajout des colonnes `stripe_customer_id` et `stripe_subscription_id` sur la table `subscriptions`, création automatique d'un Customer Stripe lors de la création d'un workspace, et mise à jour de la valeur initiale `plan_code` de STARTER à FREE avec `expires_at = now() + 14j`.

---

## Comportement attendu

### Cas nominal
1. SDK Stripe Java ajouté au `pom.xml` (stripe-java 26.3.0)
2. Migration Liquibase 020 ajoute `stripe_customer_id` et `stripe_subscription_id` (nullable) à `subscriptions`
3. À la création du workspace : subscription initialisée avec `plan_code = FREE`, `expires_at = now() + 14j`
4. `StripeCustomerService.createCustomer()` appelé après persist — `stripe_customer_id` sauvegardé si Stripe disponible
5. Si Stripe indisponible ou désactivé : fail-open, workspace créé normalement, WARN loggué

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Clé API Stripe absente / invalide | Fail-open : workspace créé, WARN loggué, `stripe_customer_id = null` | — |
| Timeout réseau vers Stripe | Fail-open : idem | — |

---

## Critères d'acceptation
- [x] La table `subscriptions` possède les colonnes `stripe_customer_id` et `stripe_subscription_id` (nullable)
- [x] Un nouveau workspace est initialisé avec `plan_code = FREE` et `expires_at = created_at + 14j`
- [x] Quand Stripe est joignable, `stripe_customer_id` est non null en base
- [x] Quand Stripe est injoignable, le workspace est créé normalement (pas d'erreur HTTP 500)
- [x] `app.stripe.enabled=false` désactive l'appel Stripe

---

## Périmètre hors-scope
- Webhook Stripe (SF-19-03)
- Checkout Session (SF-19-04)
- Logique d'expiration FREE et lecture seule (SF-19-02)
- Frontend (SF-19-05)

---

## Technique

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `subscriptions` | ALTER + UPDATE logique init | Ajout `stripe_customer_id`, `stripe_subscription_id` ; plan FREE à la création |

### Migration Liquibase
`020-add-stripe-columns-to-subscriptions.xml`

### Nouvelles classes
- `StripeCustomerService` — encapsule appel SDK Stripe, fail-open

### Classes modifiées
- `Subscription` — ajout `stripeCustomerId`, `stripeSubscriptionId`
- `WorkspaceService.createDefaultWorkspace()` — plan FREE, expires_at +14j, appel StripeCustomerService
- `PlanLimitService` — ajout limites FREE (1 dossier, 3 docs)

---

## Plan de test (réalisé — 101 tests verts)

### Tests unitaires
- [x] `StripeCustomerServiceTest` — 3 tests (désactivé / succès / exception fail-open)
- [x] `WorkspaceServiceTest` — 4 tests (FREE + expires_at / stripe_customer_id persisté / fail-open / workspace existant)
- [x] `PlanLimitServiceTest` — ajout test FREE = 1 dossier

### Tests d'intégration
- [x] `WorkspaceServiceIT` — création workspace en profil dev (Stripe désactivé)

### Isolation workspace
- [x] Chaque workspace a son propre `stripe_customer_id`
