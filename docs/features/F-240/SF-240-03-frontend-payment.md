# Mini-spec — F-240 / SF-240-03 Frontend paiement — modale CGV avant Stripe

## Identifiant

`F-240 / SF-240-03`

## Feature parente

`F-240` — Conformité contractuelle — click-wrap CGU/CGV/DPA + traçabilité

## Statut

`draft`

## Date de création

2026-05-11

## Branche Git

`feat/SF-240-03-consent-payment-frontend`

> Parallélisable avec **SF-240-02**. **Bloquée à l'intégration** par SF-240-01 (mais peut développer avec mock du service). Consomme le service `ConsentService` introduit par SF-240-02 — coordination nécessaire (SF-240-02 livre le service, SF-240-03 le consomme).

---

## Objectif

Intercaler une modale d'acceptation des CGV de paiement entre le clic "Passer au plan" et la redirection vers Stripe Checkout dans `workspace-billing.component`, et enregistrer cette acceptation côté backend via `POST /api/v1/consent/accept` avant la création de la session Stripe.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur authentifié arrive sur la page `/workspace/billing`, choisit un plan (SOLO / TEAM / PRO) et clique sur le bouton "Passer au plan X".
2. **Nouveauté** : avant l'appel actuel à `billingService.createCheckoutSession(planCode)`, ouverture d'une `MatDialog` `<app-payment-terms-acceptance-dialog>` qui affiche :
   - Titre : « Confirmer la souscription au plan {planLabel} »
   - Corps : « En souscrivant ce plan ({prix HT}/mois), j'accepte :
     - les [Conditions Générales d'Utilisation](/cgu) (ouverture nouvel onglet),
     - la [politique de confidentialité](/privacy) (ouverture nouvel onglet),
     - et le prélèvement récurrent mensuel via mon prestataire bancaire (Stripe). »
   - Checkbox bloquante : « J'accepte les conditions ci-dessus. »
   - 2 boutons : « Annuler » (`mat-button`) et « Confirmer et payer » (`mat-flat-button` + couleur primary). Le bouton "Confirmer et payer" est désactivé tant que la checkbox n'est pas cochée.
3. Si l'utilisateur **annule** : la modale se ferme, **aucun POST** consent, **aucun appel** Stripe, l'utilisateur reste sur `/workspace/billing`.
4. Si l'utilisateur **confirme** :
   - **Étape A** — Appel `consentService.acceptConsent({consentTypes: ["PAYMENT_TERMS"], version: "2026-05-11"})`.
   - **Étape B** — Si la réponse est 201, enchainer avec `billingService.createCheckoutSession(planCode)` (comportement existant).
   - **Étape C** — Si l'étape A échoue, MatSnackBar d'erreur, modale fermée, pas de redirection Stripe.
5. Le flux Stripe Checkout actuel (redirection navigateur vers `session.url`) reste inchangé.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|----------------------|
| Utilisateur annule la modale | Aucune action, aucun POST |
| Checkbox non cochée | Bouton "Confirmer et payer" désactivé visuellement |
| Erreur réseau sur `POST /consent/accept` | MatSnackBar "Impossible d'enregistrer votre acceptation, réessayer", modale fermée, pas de Stripe |
| 400 / 500 backend consent | MatSnackBar erreur générique, log console |
| Erreur `createCheckoutSession` après consent OK | Comportement existant inchangé (MatSnackBar), le consent reste enregistré |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable.
- [x] **Autres pays** : transversal — la modale s'applique à tout utilisateur (FR ou BE Wallonie/Bruxelles), texte en français en V1.
- [x] **Autres domaines** : transversal.
- [x] **Autres UI patterns** : MatDialog + MatCheckbox — patterns standard Angular Material déjà utilisés massivement dans le projet.
- [x] **Autres flows transversaux** :
  - **Auth / Principal** — sans modification.
  - **Workspace context** — l'utilisateur a un workspace primary à ce stade, donc côté backend `workspace_id` sera renseigné automatiquement.
  - **Plans / limites** — la modale s'intercale dans le flow billing existant sans toucher au gating `PlanLimitService`.
  - **Navigation / routing** — la modale est UI-only, aucune nouvelle route.

### Cas spécifique : nouveau pattern UI

Le composant introduit une `MatDialog` réutilisable pour l'acceptation contractuelle. Sa structure pourrait être généralisée si d'autres flows demandaient une acceptation similaire (ex : top-up de crédits, changement de plan downgrade avec acceptation, etc.). En V1, on garde le composant **local au module billing** sans extraction immédiate — si SF-240-04 ou une feature ultérieure demande un pattern similaire, on extraira à ce moment-là (éviter abstraction prématurée).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Top-up de crédits (`createTopupSession`) | Oui — cas similaire | **À traiter dans cette SF** : appliquer la même modale au flow top-up qui est dans le même composant. Sinon dette de convergence dans 3 mois. |
| Changement de plan downgrade | Cas hypothétique | Pas de flow distinct identifié en V1 — backlog si signal terrain |
| SF-240-02 (sign-up checkbox) | Différent — checkbox inline pas modale | Pas de partage de composant, mais partage du `ConsentService` |
| SF-240-04 (DPA téléchargement) | Différent — pas d'acceptation utilisateur, tracking serveur | Pas de partage UI |

### Décision

- [x] Étendu au flux **top-up de crédits** dans cette SF (même modale réutilisée pour `createTopupSession`).
- [x] Aucune extraction de composant partagé en V1 — composant local `payment-terms-acceptance-dialog.component.ts` dans `workspace-billing/`.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — flow billing, pas un composant décisionnel métier.

---

## Impact par domaine métier

- [x] Transversal — aucune adaptation par domaine ni par pays.

---

## Parité des domaines métier (niveau ≥ 5)

- [x] **Non applicable** — pas un outil décisionnel.

---

## Critères d'acceptation

- [ ] **CA-01** : un nouveau composant `PaymentTermsAcceptanceDialogComponent` est créé sous `frontend/src/app/workspace/workspace-billing/payment-terms-acceptance-dialog/`.
- [ ] **CA-02** : le clic sur un bouton "Passer au plan X" ouvre la modale au lieu d'appeler directement `createCheckoutSession`.
- [ ] **CA-03** : la modale affiche le nom du plan, son prix mensuel, et les 3 acceptations (CGU + Privacy + prélèvement récurrent) avec liens vers `/cgu` et `/privacy` ouverts en nouvel onglet.
- [ ] **CA-04** : le bouton "Confirmer et payer" est désactivé tant que la checkbox n'est pas cochée.
- [ ] **CA-05** : sur confirmation, `consentService.acceptConsent({consentTypes: ["PAYMENT_TERMS"], version: "2026-05-11"})` est appelé AVANT `billingService.createCheckoutSession(planCode)`.
- [ ] **CA-06** : sur annulation, ni consent ni Stripe ne sont appelés.
- [ ] **CA-07** : si le POST consent échoue, MatSnackBar et pas de redirection Stripe.
- [ ] **CA-08** : le flux **top-up** (`upgrade.topup` ou méthode `topUp` existante) reçoit la même modale (texte adapté : « Achat de crédits {N} pour {prix}€ »).
- [ ] **CA-09** : tests Jest : (a) clic ouvre modale, (b) checkbox non cochée → bouton confirm désactivé, (c) confirm appelle consent puis stripe dans l'ordre, (d) annulation = no-op, (e) erreur consent = pas de stripe.
- [ ] **CA-10** : `npm run build` reste vert. Aucune régression sur `workspace-billing.component.spec.ts`.

---

## Périmètre

### Hors scope (explicite)

- Modale en BE-NL (néerlandais) — V1 en français uniquement.
- Affichage intégral du contenu des CGV dans la modale (préférence : liens externes vers `/cgu`).
- Composant partagé extrait — V1 local au module billing.
- Mémorisation "ne plus demander pour cette session" — V1 demande systématique (chaque souscription doit être tracée).
- Confirmation par email après acceptation (Stripe envoie déjà sa confirmation, suffisant en V1).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `acceptedTerms` (checkbox) | `false` | Reset à chaque ouverture modale |

---

## Technique

### Composants Angular modifiés

- `WorkspaceBillingComponent` (`frontend/src/app/workspace/workspace-billing/workspace-billing.component.ts`) — modification de `upgrade(planCode)` et `topUp(packCode)` (si elle existe) pour ouvrir la modale avant l'appel à `billingService`.

### Composants Angular créés

- `frontend/src/app/workspace/workspace-billing/payment-terms-acceptance-dialog/payment-terms-acceptance-dialog.component.ts` + `.html` + `.scss` + `.spec.ts`. Standalone, OnPush, MatDialog, MatCheckbox. Inputs : `MAT_DIALOG_DATA` avec `planLabel: string` + `price: string` + `type: 'SUBSCRIPTION' | 'TOPUP'`. Output : `MatDialogRef<...>` returns `true` (confirmé) ou `false` (annulé).

### Services Angular consommés

- `ConsentService` (créé par SF-240-02 — coordination de merge nécessaire).
- `BillingService` (existant — inchangé).

### Endpoint(s) consommé(s)

| Méthode | URL | Provenance |
|---------|-----|------------|
| POST | `/api/v1/consent/accept` | SF-240-01 (contrat API figé) |
| POST | `/api/v1/billing/checkout-session` (existant) | Inchangé |
| POST | `/api/v1/billing/topup-session` (existant) | Inchangé |

### Migration Liquibase

- [x] Non applicable.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] **WB-01** `WorkspaceBillingComponent.spec.ts` — clic sur "Passer au plan" → `MatDialog.open` est appelé avec `PaymentTermsAcceptanceDialogComponent`.
- [ ] **WB-02** `WorkspaceBillingComponent.spec.ts` — `dialogRef.afterClosed()` retourne `true` → `consentService.acceptConsent` est appelé puis `billingService.createCheckoutSession` (assertion d'ordre).
- [ ] **WB-03** `WorkspaceBillingComponent.spec.ts` — `dialogRef.afterClosed()` retourne `false` → aucun POST.
- [ ] **WB-04** `WorkspaceBillingComponent.spec.ts` — `consentService.acceptConsent` échoue → `billingService.createCheckoutSession` n'est pas appelé + MatSnackBar.
- [ ] **WB-05** `WorkspaceBillingComponent.spec.ts` — flow top-up similaire (si méthode `topUp` existe).
- [ ] **PD-01** `PaymentTermsAcceptanceDialogComponent.spec.ts` — checkbox initialement non cochée → bouton confirm désactivé.
- [ ] **PD-02** `PaymentTermsAcceptanceDialogComponent.spec.ts` — checkbox cochée → bouton confirm actif.
- [ ] **PD-03** `PaymentTermsAcceptanceDialogComponent.spec.ts` — clic confirm → `dialogRef.close(true)`.
- [ ] **PD-04** `PaymentTermsAcceptanceDialogComponent.spec.ts` — clic annuler → `dialogRef.close(false)`.
- [ ] **PD-05** `PaymentTermsAcceptanceDialogComponent.spec.ts` — affiche bien `planLabel` et `price` reçus via `MAT_DIALOG_DATA`.

### Tests d'intégration

- [ ] Aucun (SF frontend pure).

### Isolation workspace

- [x] Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — sans modification.
- [ ] Workspace context — sans modification.
- [ ] Plans / limites — sans modification (le gating reste inchangé).
- [ ] Navigation / routing frontend — sans modification.

### Composants existants potentiellement impactés

| Composant / Endpoint | Impact | Test de non-régression |
|----------------------|--------|------------------------|
| `WorkspaceBillingComponent.spec.ts` | Les tests qui asserent le flux `upgrade()` doivent être mis à jour pour mocker la `MatDialog` | Mise à jour des fixtures dans le même PR |
| `BillingService` | Aucun (consommé inchangé) | Aucun |
| `app.routes.ts` | Aucun | Aucun |

### Smoke tests E2E concernés

- [x] `e2e/smoke/auth.spec.ts` ou un éventuel smoke billing — vérifier que le flux "Passer au plan SOLO" en staging fonctionne bout en bout avec la nouvelle modale. **Coordination requise** : le smoke test E2E doit cocher la checkbox dans la modale après le clic "Passer au plan".

---

## Dépendances

### Subfeatures bloquantes

- SF-240-01 (backend endpoint) — peut développer avec mock en attendant.
- SF-240-02 (frontend ConsentService) — partage du service. Coordination : SF-240-02 livre `ConsentService`, SF-240-03 le consomme. Si SF-240-02 n'est pas encore mergée au moment du dev SF-240-03, dupliquer la création du service dans cette branche puis dédupliquer au merge (cas rare).

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

### Décision D-01 — Modale plutôt qu'inline

Une modale est préférable à une checkbox inline sur la grille des plans pour 2 raisons :

1. **Spécificité contextuelle** : l'utilisateur voit le plan + prix exact dans la modale au moment de confirmer, ce qui rend l'engagement explicite et opposable.
2. **Pattern client SaaS B2B** : Stripe Checkout lui-même affiche un récapitulatif avant paiement. Notre modale s'inscrit dans ce pattern attendu par les avocats.

### Décision D-02 — Réutilisation pour top-up

Le top-up de crédits est juridiquement une transaction commerciale identique (engagement de paiement) à une souscription mensuelle. La même modale s'applique avec un texte adapté ("Achat de crédits N pour P €"). Économie d'effort et cohérence UX.

### Décision D-03 — Composant local, pas partagé

Le composant `PaymentTermsAcceptanceDialogComponent` reste dans `workspace/workspace-billing/` en V1. Si d'autres modules demandent une acceptation similaire (downgrade plan, changement de méthode de paiement, etc.) → extraction vers `shared/` à ce moment-là. Éviter abstraction prématurée.

### Décision D-04 — Pas de session-memory en V1

À chaque souscription, la modale s'affiche, l'acceptation est demandée et enregistrée. Conséquences :

- ✅ Trace d'audit complète (un consent par achat).
- ⚠ Friction UX si l'utilisateur souscrit plusieurs fois (rare en B2B avocats).

En V2 si signal "trop friction" : ajouter un flag `lastAcceptedAt` côté backend et raccourcir l'UX si acceptation < 30 jours (sans toutefois supprimer la modale — la traçabilité reste essentielle).
