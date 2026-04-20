# Mini-spec — F-123 / SF-123-04 Remove V1 pricing (cleanup)

## Identifiant · `F-123 / SF-123-04`
## Date · `2026-04-21` · Branche (à créer) · `feat/SF-123-04-remove-v1-pricing`

## Objectif
Retirer la double résolution V1/V2 du code : aucune souscription V1 n'existe en prod, le grandfather est superflu. On repasse à 3 price IDs (`STRIPE_PRICE_ID_{SOLO,TEAM,PRO}`) qui pointent désormais directement sur les nouveaux Prices 99/219/429 €.

## Comportement après cleanup
- `StripeCheckoutService` : 3 price IDs au lieu de 6. Nouveau checkout utilise directement `price-id-solo/team/pro`.
- `StripeWebhookService` : `resolvePlanCodeFromPriceId` matche un seul price ID par plan.
- `application.yml` : suppression de `price-id-{solo,team,pro}-v2`.
- Les env vars `STRIPE_PRICE_ID_{SOLO,TEAM,PRO}` doivent pointer sur les **nouveaux Prices** créés côté Stripe (99/219/429). Les anciens Prices (59/119/249) sont archivés côté Stripe.

## Cas d'erreur
- Env var vide → 503 explicite sur le checkout (comportement conservé)
- Webhook reçoit un price_id inconnu → fallback SOLO + warning log (comportement conservé)

## Critères d'acceptation
- [ ] `application.yml` : 3 props `price-id-*` au lieu de 6
- [ ] `StripeCheckoutService` : constructeur à 3 price IDs, suppression de `resolveV2PriceId`
- [ ] `StripeWebhookService` : constructeur à 3 price IDs, `resolvePlanCodeFromPriceId` simplifié (1 match par plan)
- [ ] Tests correspondants mis à jour (pas de nouveau test — seulement adaptation)
- [ ] Backend compile + tests verts
- [ ] Commentaires `SF-123-01 : V1 + V2` nettoyés

## Plan de test minimal
- Tests existants `StripeCheckoutServiceTest` (6) et `StripeWebhookServiceTest` (~15) passent après adaptation constructeurs
- Tests V2 spécifiques dans `StripeWebhookServiceTest` fusionnés avec les tests V1 (un seul cas par plan suffit)

## Tables / endpoints / composants impactés
- Backend : `StripeCheckoutService`, `StripeWebhookService`, `application.yml`, tests correspondants
- Pas de migration DB
- Pas de frontend

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|-------|-----------|------------|
| F-16 plans / Stripe | Nettoyage pur, pas d'impact fonctionnel | Intégré |
| Autres consumers de price IDs | Scan : aucun hors des 2 services Stripe | Non applicable |

Pas de nouveau pattern partagé.

## Préoccupations transversales
- **Plans / limites** : aucune gate modifiée
- **Auth / Principal** : inchangé
- **Workspace context** : inchangé

## Hors scope
- Migration des données (aucune Subscription V1 en base)
- Changement de comportement fonctionnel
- Côté Stripe : création des nouveaux Prices + archivage des anciens = opération manuelle documentée dans la PR

## Action opérateur (hors code)
Avant le merge (ou juste après) :
1. Stripe Dashboard → Products → sur chacun des 3 Products existants : + Add a new price (99/219/429 €, configuration graduated détaillée dans SF-123-01)
2. Archiver les 3 anciens Prices (59/119/249 €)
3. Mettre à jour `STRIPE_PRICE_ID_{SOLO,TEAM,PRO}` en staging et prod avec les nouveaux price_id
4. Rollout restart backend
