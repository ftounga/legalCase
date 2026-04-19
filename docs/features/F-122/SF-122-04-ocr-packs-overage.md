# Mini-spec — F-122 / SF-122-04 Packs overage OCR Stripe + section billing

## Identifiant
`F-122 / SF-122-04`

## Feature parente
`F-122` — OCR pour PDF scannés

## Statut `draft`  · Date `2026-04-19`  · Branche `feat/SF-122-04-ocr-packs-overage`

---

## Objectif

Permettre à l'avocat d'acheter des packs OCR supplémentaires quand son quota mensuel est atteint. Même plomberie Stripe que F-49 (tokens one-shot). `PlanLimitService.isOcrQuotaExceeded` étendu pour considérer le reliquat de pages achetées comme "credits restants".

---

## Comportement

**Packs Stripe** :
- `OCR_500` — 500 pages — 19 € (marge 64 %)
- `OCR_2000` — 2 000 pages — 59 € (marge 53 %)
- `OCR_8000` — 8 000 pages — 199 € (marge 45 %)

**Endpoint existant réutilisé** : `POST /api/v1/stripe/topup-session` avec `packCode=OCR_500|OCR_2000|OCR_8000` (dispatch par prefix dans `TopupCheckoutService`).

**Webhook** : `checkout.session.completed` mode `payment`, metadata `pack_code=OCR_*` → `CreditPurchaseService.recordOcrPack(workspaceId, pages, amountCents, sessionId)`.

**Gate extension** : `isOcrQuotaExceeded` calcule `remainingBoughtPages = totalBought - max(0, allTimeUsed - monthsActive × planMonthlyPages)`. Pages effectives disponibles = `planMonthlyPages + remainingBoughtPages`. Si `effectiveMonthUsage + additionalPages > effective` ET `effectiveDayUsage + additionalPages > 500` (hard cap inchangé) → `OCR_QUOTA_EXCEEDED`.

**Frontend** — section "OCR" dans `/workspace/billing` :
- Compteur "X / Y pages utilisées ce mois" (where Y = plan + remaining packs)
- Bouton "Acheter des pages OCR" → modale 3 packs avec tarif
- Clic pack → appel `/topup-session` → redirect Stripe Checkout

---

## Critères d'acceptation

- [ ] Migration 084 : `workspaces.ocr_pages_used_all_time`, `credit_purchases.ocr_pages_bought`
- [ ] `OcrPack` enum (OCR_500/2000/8000) avec pages + amountCents
- [ ] `application.yml` : 3 price IDs `app.stripe.price-id-ocr-{500,2000,8000}`
- [ ] `TopupCheckoutService` dispatch OCR vs TOKENS par prefix
- [ ] `StripeWebhookService` détecte `OCR_*`, appelle `recordOcrPack`
- [ ] `CreditPurchaseService.recordOcrPack` persiste avec `ocrPagesBought=N, tokensBought=0`
- [ ] `CreditPurchaseService.getTotalOcrPagesBought(wid)` somme
- [ ] `WorkspaceRepository.incrementOcrUsage` incrémente aussi `all_time`
- [ ] `PlanLimitService.isOcrQuotaExceeded` considère le reliquat acheté
- [ ] Section billing frontend avec 3 boutons + appel existant
- [ ] Tests : 8+ nouveaux backend, 3+ frontend

## Hors scope

- Notification email "quota presque atteint" — V2
- Affichage détaillé de la consommation historique (graph) — V2
- Remboursement / expiration des packs — V2 (packs illimités dans le temps)

---

## Technique

| Fichier | Opération |
|---|---|
| `backend/src/main/resources/db/changelog/migrations/084-add-ocr-credits-columns.xml` | NOUVEAU |
| `backend/src/main/java/fr/ailegalcase/billing/OcrPack.java` | NOUVEAU |
| `backend/src/main/java/fr/ailegalcase/billing/TopupCheckoutService.java` | dispatch OCR prefix + 3 price IDs |
| `backend/src/main/java/fr/ailegalcase/billing/StripeWebhookService.java` | détecte OCR_* |
| `backend/src/main/java/fr/ailegalcase/billing/CreditPurchase.java` | + field ocrPagesBought |
| `backend/src/main/java/fr/ailegalcase/billing/CreditPurchaseRepository.java` | + sumOcrPagesBought |
| `backend/src/main/java/fr/ailegalcase/billing/CreditPurchaseService.java` | + getTotalOcrPagesBought + recordOcrPack |
| `backend/src/main/java/fr/ailegalcase/workspace/Workspace.java` | + ocrPagesUsedAllTime |
| `backend/src/main/java/fr/ailegalcase/workspace/WorkspaceRepository.java` | incrementOcrUsage + all_time |
| `backend/src/main/java/fr/ailegalcase/billing/PlanLimitService.java` | isOcrQuotaExceeded + reliquat |
| `backend/src/main/resources/application.yml` | 3 price IDs |
| `frontend/src/app/workspace/workspace-billing/*` | section OCR + boutons |
| Tests backend + frontend |

---

## Analyse d'impact

- [ ] Auth : non touché
- [ ] Workspace context : + 1 colonne (non structurel)
- [x] **Plans / limites** : étendu (reliquat packs) — composants impactés : `OcrService` (déjà consumer), `PlanLimitService`, `CreditPurchaseService`
- [ ] Navigation : non touché

Aucun smoke E2E concerné.
