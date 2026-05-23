# SF-255-04 — Backend Stripe Coupons + PromotionCodes (STRIPE_DISCOUNT)

## Objectif

Activer la branche `STRIPE_DISCOUNT` des codes promo : à la création d'un code de ce type par un super-admin, créer un Coupon + PromotionCode côté Stripe ; activer la saisie native de codes promo dans Stripe Checkout ; synchroniser les redemptions via webhook `customer.discount.created`.

## Contexte

F-255 = codes promo. Les SF-01/02/03 (TRIAL_EXTENSION, écran admin, écran billing) sont mergées. La branche `STRIPE_DISCOUNT` retournait jusqu'ici `409 PROMO_CODE_TYPE_NOT_SUPPORTED_YET` sur redemption. SF-04 lève ce blocage en s'appuyant sur l'API Stripe.

## Décision d'architecture (validée step 0)

L'utilisateur final saisit son code STRIPE_DISCOUNT **dans Stripe Checkout** (champ natif `allow_promotion_codes=true`), PAS dans `/workspace/billing`. Cette page reste réservée à TRIAL_EXTENSION (SF-03). Justifications :
- Évite la double saisie / double UI à maintenir.
- Stripe gère nativement validation, comptage, audit, expiration côté payment.
- Le code d'erreur `PROMO_CODE_TYPE_NOT_SUPPORTED_YET` à l'endpoint `/workspace/billing/promo-codes/redeem` reste tel quel — c'est volontaire (filet de sécurité contre l'usage incorrect).

## Comportement nominal

### Création d'un code STRIPE_DISCOUNT (super-admin)

`POST /api/v1/super-admin/promo-codes` avec :
```json
{
  "code": "PARTNER10",
  "type": "STRIPE_DISCOUNT",
  "valueOffType": "PERCENT",     // ou "AMOUNT"
  "valueOffAmount": 10,           // pourcentage 1-100 si PERCENT, sinon centimes EUR
  "currency": "EUR",              // requis si AMOUNT, ignoré si PERCENT
  "duration": "ONCE",             // ou "REPEATING_3" ou "FOREVER"
  "partnerLabel": "Partenaire X",
  "maxUses": 100,
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

Effet :
1. Création locale (atomique) : `PromoCode` avec les 5 nouveaux champs (`valueOffType`, `valueOffAmount`, `currency`, `duration`, `stripePromotionCodeId`).
2. Appel Stripe : `Coupon.create()` + `PromotionCode.create()` — référence par `code` (uppercase).
3. INSERT local avec `stripe_coupon_id` (existant) et `stripe_promotion_code_id` (nouveau).
4. Réponse `201 Created` avec `PromoCodeDto`.

Si Stripe échoue : `502 STRIPE_API_UNAVAILABLE`, aucun INSERT local (transactionnel).

### Désactivation d'un code STRIPE_DISCOUNT

`POST /api/v1/super-admin/promo-codes/{id}/deactivate` — flip local `active=false` + tentative best-effort `PromotionCode.update(active=false)` côté Stripe. Si Stripe échoue : log warning, 200 OK quand même (la désactivation locale est suffisante).

### Application par l'utilisateur (Stripe Checkout)

`StripeCheckoutService.createCheckoutSession` et `createSubscriptionSessionForNewWorkspace` activent désormais `setAllowPromotionCodes(true)` sur le `SessionCreateParams.Builder`. Le user saisit son code dans le champ Stripe Checkout — Stripe valide / applique / refuse selon la définition du Coupon.

### Synchronisation webhook (redemption)

Nouveau case dans `StripeWebhookService.handleEvent` : `customer.discount.created`.
- Lookup local : `PromoCodeRepository.findByStripePromotionCodeId(promotionCodeId)`.
- Si trouvé : INSERT `PromoCodeRedemption` (workspace_id résolu via `Subscription.stripeCustomerId`) + incrément `uses_count` atomique.
- Idempotence : colonne `source_event_id` UNIQUE NULL sur `promo_code_redemptions` — si Stripe ré-émet le même event, on no-op.

## Cas d'erreur

| Code | HTTP | Quand |
|------|------|-------|
| `STRIPE_API_UNAVAILABLE` | 502 | Stripe down à la création (aucun INSERT local) |
| `PROMO_CODE_TYPE_NOT_SUPPORTED_YET` | 409 | User tente de saisir un STRIPE_DISCOUNT dans `/workspace/billing/promo-codes/redeem` (conservé) |
| Validation Bean | 400 | `STRIPE_DISCOUNT` sans `valueOffType` / `valueOffAmount` / `duration`, ou avec `valueDays` (interdit pour ce type) |

## Critères d'acceptation

1. Création STRIPE_DISCOUNT percent_off nominale → `Coupon.create()` + `PromotionCode.create()` appelés avec les bons params, INSERT local avec `stripe_promotion_code_id` non null.
2. Création STRIPE_DISCOUNT amount_off + currency → idem en mode amount.
3. Création STRIPE_DISCOUNT sans `valueOffType` → `400`.
4. Création STRIPE_DISCOUNT avec `valueDays` → `400`.
5. Création STRIPE_DISCOUNT, Stripe lève `StripeException` → `502 STRIPE_API_UNAVAILABLE`, aucun INSERT local.
6. Désactivation STRIPE_DISCOUNT : flip local + `PromotionCode.update(active=false)` appelé.
7. Désactivation, Stripe échoue : flip local quand même, log warning, 200 OK.
8. `StripeCheckoutService` : `setAllowPromotionCodes(true)` appelé sur les 2 builders.
9. Webhook `customer.discount.created` nominal : INSERT redemption + `uses_count` incrémenté.
10. Webhook avec `promotion_code` inconnu local : log warning, pas d'INSERT, pas d'exception.
11. Webhook ré-émis (même `event_id`) : idempotent, pas de 2e INSERT.
12. TRIAL_EXTENSION reste 100% inchangé (zéro appel Stripe, tests SF-01 verts).

## Plan de test

### Unitaires
- `StripePromoCodeServiceTest` (nouveau, MockedStatic Coupon + PromotionCode) :
  - création percent
  - création amount
  - désactivation
  - désactivation Stripe échoue → log warning, pas d'exception
- `PromoCodeServiceTest` (étendu) :
  - création STRIPE_DISCOUNT nominale → mock `StripePromoCodeService`
  - validation : STRIPE_DISCOUNT + `valueDays` non null → 400
  - validation : STRIPE_DISCOUNT sans `valueOffType` → 400
  - Stripe lève → 502 `STRIPE_API_UNAVAILABLE`
  - désactivation propagée
  - désactivation Stripe échoue → flip local OK
- `StripeCheckoutServiceTest` (étendu) :
  - vérif `setAllowPromotionCodes(true)` sur les 2 méthodes (via `ArgumentCaptor<SessionCreateParams>`)
- `StripeWebhookServiceTest` (étendu) :
  - event nominal
  - event avec promotion_code inconnu
  - idempotence event_id

### Intégration
- `PromoCodeAdminControllerIT` (étendu) : POST création STRIPE_DISCOUNT (avec `StripePromoCodeService` mocké via `@MockBean`) → 201 + vérif DB.

## Tables / endpoints / composants impactés

### Migrations Liquibase
- `301-add-stripe-fields-to-promo-codes.xml` — 5 colonnes nullables sur `promo_codes` + index sur `stripe_promotion_code_id`.
- `302-add-value-applied-amount-to-promo-code-redemptions.xml` — colonnes `value_applied_amount` + `source_event_id` (UNIQUE NULL) sur `promo_code_redemptions`.

### Entités
- `PromoCode` — 5 nouveaux champs.
- `PromoCodeRedemption` — 2 nouveaux champs.
- `PromoCodeValueOffType` (nouvel enum) : PERCENT, AMOUNT.
- `PromoCodeDuration` (nouvel enum) : ONCE, REPEATING_3, FOREVER.

### Services
- `StripePromoCodeService` (nouveau) — wrapper Coupon + PromotionCode.
- `PromoCodeService` — validation + branchement Stripe à `createCode` / `deactivateCode`.
- `StripeCheckoutService` — `setAllowPromotionCodes(true)` sur les 2 builders.
- `StripeWebhookService` — case `customer.discount.created`.

### DTOs
- `PromoCodeCreateRequest` — 4 champs nullables.
- `PromoCodeDto` — reflet.

### Repositories
- `PromoCodeRepository.findByStripePromotionCodeId` — nouveau.
- `PromoCodeRedemptionRepository.findBySourceEventId` — nouveau.

### Codes d'erreur
- `STRIPE_API_UNAVAILABLE` — 502, Stripe down à la création.

## Hors périmètre

- Saisie du code dans `/workspace/billing` pour STRIPE_DISCOUNT (volontairement non supporté — Stripe Checkout est l'unique point d'entrée).
- Multi-devise (V1 : EUR uniquement).
- Coupon par produit ou par plan (cf. `applies_to` Stripe — V2 si besoin).
- Promotion Codes per-customer (V2).

## Compatibilité descendante

- Tous les tests SF-01/02/03 restent verts.
- TRIAL_EXTENSION : aucun appel Stripe, aucun changement de comportement.
- L'endpoint `/workspace/billing/promo-codes/redeem` retourne toujours `409 PROMO_CODE_TYPE_NOT_SUPPORTED_YET` pour STRIPE_DISCOUNT (filet de sécurité).

## Liens

- Stripe Coupons API : https://stripe.com/docs/api/coupons
- Stripe Promotion Codes : https://stripe.com/docs/api/promotion_codes
- SF-01 backend TRIAL_EXTENSION : PR #1253
- SF-02 frontend admin : PR #1255
- SF-03 frontend user : PR #1256
