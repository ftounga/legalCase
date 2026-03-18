# Mini-spec — SF-19-04 — Endpoint Checkout Session Stripe (backend)

## Identifiant
`SF-19-04`

## Feature parente
`F-19` — Intégration paiement Stripe

## Statut
`in-progress`

## Date de création
2026-03-18

## Branche Git
`feat/SF-19-04-checkout-session`

---

## Objectif
Exposer un endpoint POST qui crée une Stripe Checkout Session pour le workspace courant et retourne l'URL de paiement vers laquelle rediriger l'utilisateur.

---

## Comportement attendu

### Cas nominal
1. L'utilisateur authentifié POST `/api/v1/stripe/checkout-session` avec `{ "planCode": "STARTER" }` ou `"PRO"`
2. Le backend récupère la subscription du workspace courant
3. Si `stripe_customer_id` est null → appel `StripeCustomerService.createCustomer()` et sauvegarde
4. Création d'une Checkout Session Stripe avec le `price_id` correspondant au plan
5. `success_url` = `${app.frontend-url}/workspace/billing?success=true`
6. `cancel_url` = `${app.frontend-url}/workspace/billing?canceled=true`
7. Retour `200` avec `{ "checkoutUrl": "https://checkout.stripe.com/..." }`

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `app.stripe.enabled=false` | Erreur explicite | 503 |
| `planCode` absent ou invalide (pas STARTER/PRO) | Erreur explicite | 400 |
| Erreur Stripe lors de la création session | Log ERROR + message générique | 502 |
| Workspace sans subscription | Erreur explicite | 404 |

---

## Critères d'acceptation

- [ ] POST `/api/v1/stripe/checkout-session` avec planCode=STARTER → retourne checkoutUrl valide
- [ ] POST `/api/v1/stripe/checkout-session` avec planCode=PRO → retourne checkoutUrl valide
- [ ] stripe.enabled=false → 503
- [ ] planCode invalide → 400
- [ ] Si stripe_customer_id absent → customer créé et sauvegardé avant la session
- [ ] Erreur Stripe → 502
- [ ] Endpoint accessible uniquement aux rôles OWNER/ADMIN

---

## Périmètre

### Hors scope
- Frontend (SF-19-05)
- Page billing/pricing Angular
- Bannière trial
- Gestion retour Stripe post-paiement (géré par webhook SF-19-03)

---

## Technique

### Endpoint

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/stripe/checkout-session` | Oui | OWNER |

### Request body
```json
{ "planCode": "STARTER" }
```

### Response body
```json
{ "checkoutUrl": "https://checkout.stripe.com/pay/cs_live_..." }
```

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `subscriptions` | SELECT + UPDATE conditionnel | Mise à jour stripe_customer_id si absent |

### Migration Liquibase
Non applicable

### Nouvelles classes
- `StripeCheckoutService` — logique création session
- `StripeCheckoutController` — endpoint POST

---

## Plan de test

### Tests unitaires
- [ ] `StripeCheckoutService` — nominal STARTER → checkoutUrl retourné
- [ ] `StripeCheckoutService` — nominal PRO → checkoutUrl retourné
- [ ] `StripeCheckoutService` — stripe.enabled=false → exception 503
- [ ] `StripeCheckoutService` — planCode inconnu → exception 400
- [ ] `StripeCheckoutService` — stripe_customer_id absent → customer créé avant session
- [ ] `StripeCheckoutService` — erreur Stripe → exception 502

### Tests d'intégration
- [ ] POST sans auth → 401
- [ ] POST avec planCode invalide → 400
- [ ] POST stripe disabled → 503

### Isolation workspace
- [ ] Applicable — le workspace_id est toujours celui du user connecté, jamais un paramètre libre

---

## Dépendances
- SF-19-01 (StripeCustomerService) — `done`
- SF-19-03 (webhook) — `done`
