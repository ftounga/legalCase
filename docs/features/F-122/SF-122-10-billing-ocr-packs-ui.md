# Mini-spec — F-122 / SF-122-10 Packs OCR achetables dans la page billing

## Identifiant
`F-122 / SF-122-10`

## Feature parente
`F-122` — OCR pour PDF scannés

## Statut `draft`  · Date `2026-04-19`  · Branche `feat/SF-122-10-billing-ocr-packs-ui`

---

## Objectif

Exposer les 3 packs OCR (OCR_500 / OCR_2000 / OCR_8000) à l'achat dans la page `/workspace/billing`. La plomberie backend + Stripe a été livrée dans SF-122-04 mais la sélection UI n'a jamais été ajoutée — aucun avocat ne peut acheter de pages OCR aujourd'hui, même quand son quota mensuel est épuisé.

---

## Comportement

### Section topup réorganisée en 2 catégories

Dans `workspace-billing.component.html`, la section unique "Acheter des tokens supplémentaires" est scindée en deux :

1. **Packs tokens** (existants, inchangés)
   - TOKENS_1M — 9,90 €
   - TOKENS_5M — 39,90 €
   - TOKENS_20M — 129,90 €

2. **Packs OCR** (nouveaux)
   - OCR_500 — 500 pages — 19,00 €
   - OCR_2000 — 2 000 pages — 59,00 €
   - OCR_8000 — 8 000 pages — 199,00 €

Chaque carte pack OCR affiche : icône, label, nombre de pages, prix, bouton "Acheter".

### Handler d'achat

Le bouton "Acheter" appelle `buyTopup('OCR_500'|'OCR_2000'|'OCR_8000')`. La méthode existante dispatche automatiquement côté backend (`TopupCheckoutService` matche le prefix `OCR_`), aucun changement backend.

### Snackbar de retour

Le `queryParams.topup` existant (`success` / `canceled`) couvre déjà les deux flows. On ajoute la détection du type OCR via un paramètre `topup_kind=ocr` pour afficher un message adapté :

- `?topup=success&topup_kind=ocr` → "Pages OCR ajoutées à votre quota !"
- `?topup=canceled&topup_kind=ocr` → "Achat de pages OCR annulé."
- `?topup=success` sans kind → message tokens (comportement actuel préservé)

Le `success_url` / `cancel_url` côté `TopupCheckoutService` est étendu pour transporter `topup_kind=ocr` quand le packCode commence par `OCR_`.

### Cas d'erreur

- Stripe désactivé (`app.stripe.enabled=false`) : backend renvoie 403/409 existant → snackbar d'erreur existant (pas de changement)
- Price ID OCR non configuré : backend renvoie 500 existant → snackbar d'erreur existant

---

## Critères d'acceptation

- [ ] 3 cartes OCR_500 / OCR_2000 / OCR_8000 visibles dans `/workspace/billing`, séparées visuellement des packs tokens par un titre "Packs OCR"
- [ ] Clic "Acheter" sur OCR_500 → redirection Stripe Checkout (vérifié en staging avec vrai price ID)
- [ ] Retour de Stripe après succès OCR → snackbar "Pages OCR ajoutées à votre quota !"
- [ ] Retour de Stripe après annulation OCR → snackbar "Achat de pages OCR annulé."
- [ ] Les packs tokens restent fonctionnels et affichent le snackbar original
- [ ] Aucune régression sur les plan cards (FREE/SOLO/TEAM/PRO)

---

## Plan de test

### Unitaires frontend

- `workspace-billing.component.spec.ts` — existant, vérifier `packs` tableau contient bien les 3 nouveaux OCR + les 3 tokens
- Nouveau test : `buyTopup('OCR_500')` déclenche bien l'appel au service avec le bon code
- Nouveau test : `queryParams.topup_kind=ocr` + `topup=success` → snackbar OCR-spécifique

### Unitaires backend

- `TopupCheckoutServiceTest` : ajouter un test que le `success_url` / `cancel_url` contient `topup_kind=ocr` quand packCode commence par `OCR_`
- Vérifier rétrocompat : packCode TOKENS_* ne met pas `topup_kind` dans l'URL

### Intégration

- Smoke test manuel en staging : achat OCR_500 avec carte test Stripe → vérifier webhook → vérifier `ocr_pages_bought` incrémenté → vérifier snackbar

### Isolation workspace

- Non applicable — l'URL Stripe Checkout est déjà scope workspace via `workspaceId` dans metadata.

---

## Tables / endpoints / composants impactés

### Backend
- `TopupCheckoutService.java` — ajout `topup_kind=ocr` dans success/cancel URL quand OCR_*

### Frontend
- `workspace-billing.component.ts` — tableau `packs` étendu + nouvelle prop `category` ('tokens' | 'ocr')
- `workspace-billing.component.html` — deux sections topup distinctes
- `workspace-billing.component.scss` — styles section "Packs OCR" (reprend styles existants)

### Config
- Aucun changement — les 3 price IDs Stripe OCR sont déjà en secret (configurés lors de SF-122-04)

---

## Hors périmètre

- Quota OCR affiché dans les plan cards (→ SF-122-11)
- Progress bar OCR dans workspace-admin (→ SF-122-12)
- Affichage du solde actuel de pages OCR dans la page billing (V2, viendrait naturellement avec SF-122-12)
- Notifications email quand pack épuisé (V2)

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Autres pays (Belgique) | Oui | **Intégrée** — mêmes packs, mêmes prix, aucune distinction pays (tarification Stripe unique) |
| Autres domaines (immigration/famille) | Oui | **Intégrée** — les packs sont workspace-scope, pas domain-scope |
| Autres outils / comparateurs | Non | packs OCR ne concernent que l'extraction documentaire |
| Autres plans | Non | packs = overage universel, pas restreint à un plan |

**Analyse d'impact cross-cutting** :
- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [ ] Plans / limites — **déjà intégré dans SF-122-04** (reliquat packs calculé correctement par `PlanLimitService`)
- [ ] Navigation / routing — non touché

Aucun smoke E2E concerné.

---

## Nouveau pattern UI ou service partagé

- [x] Pas de nouveau pattern — réutilisation stricte de la carte topup existante (structure `topup-card` identique). Juste une duplication section → ce n'est **pas** une abstraction, c'est de la répétition locale justifiée (3 lignes HTML ajoutées).
- [x] Pas de service partagé nouveau — `BillingService.createTopupSession()` existant réutilisé tel quel.
