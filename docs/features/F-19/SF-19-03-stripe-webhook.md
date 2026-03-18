# Mini-spec — SF-19-03 — Webhook Stripe : réception événements et mise à jour plan automatique

## Identifiant
`SF-19-03`

## Feature parente
`F-19` — Intégration paiement Stripe

## Statut
`done`

## Date de création
2026-03-18

## Branche Git
`feat/SF-19-03-stripe-webhook`

---

## Objectif
Endpoint webhook public qui reçoit les événements Stripe, vérifie leur signature, et met à jour automatiquement le plan de la subscription correspondante.

---

## Critères d'acceptation
- [x] POST /api/v1/stripe/webhook accessible sans auth
- [x] Signature invalide → 400
- [x] checkout.session.completed → plan mis à jour
- [x] customer.subscription.deleted → FREE + expires_at = now()
- [x] Customer inconnu → 200, log WARN
- [x] Événement non géré → 200 sans erreur

---

## Technique

### Endpoint
POST /api/v1/stripe/webhook — public, body raw byte[]

### Nouvelles classes
- `StripeWebhookService` — logique traitement événements
- `StripeWebhookController` — endpoint POST, vérification signature
- `SubscriptionRepository.findByStripeCustomerId()`

### Classes modifiées
- `SecurityConfig` — permitAll sur /api/v1/stripe/webhook
- `application.yml` — price-id-starter, price-id-pro

### Tests (115/115 verts)
- `StripeWebhookServiceTest` — 7 tests unitaires
- `StripeWebhookControllerIT` — 2 tests intégration (no auth / signature invalide)
