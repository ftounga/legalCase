# Mini-spec — F-123 / SF-123-01 Stripe V2 prices + grandfather

## Identifiant · `F-123 / SF-123-01`
## Date · `2026-04-20` · Branche · `feat/SF-123-01-stripe-v2-prices`

## Objectif
Basculer la grille tarifaire V1 (SOLO 59 € · TEAM 119 € · PRO 249 € — ou V1 historique 40-49 € selon clients) vers la grille V2 (SOLO 99 € · TEAM 219 € · PRO 429 €) sur les **nouveaux checkouts**, tout en **préservant** tarif et expérience des clients existants (grandfather natif Stripe).

## Arbitrage clé : grandfather sans colonne DB
Stripe stocke déjà le `price_id` sur chaque `Subscription`, immuable jusqu'à modification explicite. Donc tant qu'on ne recrée pas la sub, les clients V1 restent facturés au tarif V1 automatiquement. **Pas besoin d'une colonne `price_id_immutable` en DB** — on étend simplement le mapping `price_id → plan_code` pour reconnaître V1 ET V2 côté webhook. Approche rétrocompat 100%, zéro migration données.

## Scope

### Backend
- `application.yml` + `StripeProperties` : ajouter `price-id-solo-v2`, `price-id-team-v2`, `price-id-pro-v2` (placeholders, à peupler via env vars en staging/prod)
- `StripeCheckoutService.resolvePriceId(planCode)` : retourne désormais **V2** pour les nouveaux checkouts
- `StripeWebhookService.resolvePlanCodeFromPriceId(priceId)` : reconnaît **V1 ET V2** via un map enrichi ; rétrocompat clients existants qui restent sur leurs V1 price IDs Stripe
- Tests unitaires : 4 cas (checkout V2 par plan, webhook résolution V1 historique, webhook résolution V2 nouveau, prix inconnu → IllegalArgumentException)

### Frontend
- `workspace-billing.component.ts` : grille `plans[]` mise à jour (SOLO 99 / TEAM 219 / PRO 429) + note "Nouveau tarif applicable aux nouvelles souscriptions. Votre plan actuel conserve son tarif d'origine jusqu'à votre prochain changement."
- `landing/index.html` : section pricing réécrite (3 cartes alignées sur le scénario A)
- Tests : spec existant mis à jour pour nouveaux prix

### Déploiement (hors code, à documenter côté user)
- Créer dans Stripe Dashboard 3 nouveaux Prices :
  - SOLO V2 : 99 €/mois
  - TEAM V2 : 219 €/mois (quantity-based pour SF-123-02 plus tard, pour SF-123-01 on reste en fixed amount — 3 users inclus)
  - PRO V2 : 429 €/mois (5 users inclus — idem)
- Récupérer les `price_id` (format `price_XXXXX`) et les setter dans `STRIPE_PRICE_ID_SOLO_V2`, `_TEAM_V2`, `_PRO_V2` sur staging puis prod
- Ne **pas** toucher aux price IDs V1 — ils restent actifs pour les clients existants

## Scénario A retenu

| Plan | Prix V1 | Prix V2 | Users inclus V2 |
|---|---|---|---|
| FREE | 0 € | 0 € | 1 |
| SOLO | 59 €/mois | **99 €/mois** | 1 |
| TEAM | 119 €/mois | **219 €/mois** | 3 inclus (cap 6 via SF-123-02) |
| PRO | 249 €/mois | **429 €/mois** | 5 inclus |

Les colonnes "users inclus" entrent réellement en jeu dans SF-123-02 (backend per-user). Pour SF-123-01, on reste en `fixed price` simple — TEAM = 219 € quel que soit le nombre d'utilisateurs (cap géré en SF-123-02).

## Critères d'acceptation
- [x] `application.yml` : props `stripe.price-id-solo-v2/team-v2/pro-v2` (defaults vides, à peupler par env)
- [x] `StripeCheckoutService.resolvePriceId` retourne V2 pour SOLO/TEAM/PRO
- [x] `StripeWebhookService.resolvePlanCodeFromPriceId` reconnaît V1 ET V2 pour chaque plan
- [x] Tests unitaires Webhook : V1 SOLO → SOLO, V2 SOLO → SOLO, V1 TEAM → TEAM, V2 TEAM → TEAM, V1 PRO → PRO, V2 PRO → PRO, inconnu → null
- [x] Tests unitaires Checkout : SOLO → V2 price ID, TEAM → V2 price ID, PRO → V2 price ID
- [x] Frontend workspace-billing : prix V2 + note grandfather
- [x] Frontend landing : grille V2 cohérente
- [x] 984/984 backend + 1063/1063 frontend verts (ou delta expliqué)

## Hors scope explicite
- Per-user billing (quantity Stripe) → **SF-123-02**
- Modal invitation avec coût visible → **SF-123-03**
- Migration Stripe active (recréation de subscriptions V1 → V2) → jamais fait (casserait le grandfather, viole l'intention produit)
- Communication landing "Pourquoi ce prix" avec comparatif concurrents → bonus, peut être ajouté en SF follow-up si l'angle commercial le justifie

## Analyse transversale
- Auth / Principal / Workspace context : non touché
- Plans / limites : **lu** seulement (SOLO/TEAM/PRO inchangés comme codes), pas de nouveau plan
- Navigation / routing : non
- Préoccupations transversales : aucune nouvelle

## Risques
- **R1** : si un avocat en V1 fait un upgrade SOLO → TEAM, Stripe passera sa subscription sur le TEAM V2 price ID (nouveau tarif). C'est conforme à l'intention (montée en gamme = tarif actuel).
- **R2** : si un client V1 veut downgrade et remonter, il remontera en V2. Acceptable — communiqué proactivement dans la note d'en-tête workspace-billing.
- **R3** : si `STRIPE_PRICE_ID_*_V2` ne sont pas setup en prod, les nouveaux checkouts crasheront. Mitigation : exception explicite (`IllegalArgumentException`) au démarrage si enabled mais vides. Ajouté dans la logique `resolvePriceId`.
