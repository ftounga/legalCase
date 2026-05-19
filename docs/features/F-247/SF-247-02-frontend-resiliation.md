# Mini-spec — F-247 / SF-247-02 — Frontend résiliation d'abonnement self-service

## Identifiant

`F-247 / SF-247-02`

## Feature parente

`F-247` — Résiliation d'abonnement en self-service

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-247-02-frontend-cancel-subscription`

---

## Objectif

Permettre à l'OWNER de résilier (et de réactiver) son abonnement depuis l'écran Abonnement, et signaler clairement l'état « résiliation programmée ».

---

## Comportement attendu

### Cas nominal

1. À l'ouverture de `/workspace/billing`, le composant appelle `GET /api/v1/billing/subscription`.
2. **Si une résiliation est programmée** (`cancelAtPeriodEnd === true`) : un bandeau s'affiche en **haut** de l'écran (pattern `expired-banner`) : « Votre abonnement reste actif jusqu'au {currentPeriodEnd | date} » + bouton « Réactiver l'abonnement ».
3. **Si le plan est payant, pas de résiliation programmée, et l'utilisateur est OWNER** : une section discrète « Résilier l'abonnement » s'affiche en **bas** de l'écran, sous les sections d'achat, avec un bouton dé-emphasé (`mat-stroked-button`).
4. Clic « Résilier » → `MatDialog` de confirmation rappelant : résiliation en fin de période, accès conservé jusqu'au {date}, données conservées, passage en FREE ensuite.
5. Confirmation → `POST /api/v1/billing/cancel` → au succès, le bandeau « résiliation programmée » remplace la section, `MatSnackBar` de confirmation.
6. Clic « Réactiver » → `POST /api/v1/billing/resume` → au succès, le bandeau disparaît, la section « Résilier » réapparaît, `MatSnackBar`.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Appel `cancel`/`resume` en échec (`403`/`409`/`502`) | `MatSnackBar` avec le message du backend (`error.error.message`), aucun changement d'état optimiste non confirmé |
| `GET subscription` en échec | la section résiliation n'est pas affichée (fail-safe), pas de blocage de l'écran billing |
| Utilisateur non-OWNER | la section « Résilier » n'est pas rendue (le bandeau « résiliation programmée », lui, reste visible pour information) |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier / pays / domaines** — non applicable : écran de billing transversal.
- [x] **Autres UI patterns** — réutilise le pattern `expired-banner` (bandeau haut d'écran) déjà présent dans `workspace-billing.component.html` ; réutilise `MatDialog` de confirmation (pattern existant, ex. retrait de membre F-123). Aucun nouveau pattern partagé introduit.
- [x] **Autres flows transversaux** — Navigation : aucune nouvelle route (section dans un écran existant).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| `expired-banner` (bandeau billing) | Oui | Pattern réutilisé pour le bandeau « résiliation programmée ». |
| `MatDialog` de confirmation | Oui | Pattern réutilisé, pas de composant partagé nouveau. |

### Décision

- [x] Étendu à toutes les cibles applicables ; aucun pattern partagé nouveau → pas de dette de convergence.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : composant d'écran billing, ce n'est pas une section décisionnelle `<app-XXX-section>` intégrée au panel F-IA-04 / `TOOL_REGISTRY`.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : écran de billing, aucun champ saisissable pré-rempli par l'IA.

---

## Critères d'acceptation

- [ ] Sur un workspace payant sans résiliation programmée, l'OWNER voit la section « Résilier l'abonnement » en bas de l'écran billing.
- [ ] Un membre non-OWNER ne voit pas la section « Résilier ».
- [ ] Le clic « Résilier » ouvre un dialog de confirmation ; l'annulation du dialog n'appelle pas le backend.
- [ ] Après confirmation, un appel `POST /api/v1/billing/cancel` est émis ; au succès, le bandeau « résiliation programmée » s'affiche en haut avec la date.
- [ ] Le bouton « Réactiver » appelle `POST /api/v1/billing/resume` ; au succès le bandeau disparaît.
- [ ] Une erreur backend affiche un `MatSnackBar` avec le message renvoyé.
- [ ] Sur un workspace FREE, aucune section résiliation ni bandeau n'est rendu.

---

## Périmètre

### Hors scope (explicite)

- Toute logique Stripe (SF-247-01).
- Enquête de départ / churn survey.
- Écran de préférences dédié.

---

## Technique

### Composants Angular

- `WorkspaceBillingComponent` — ajout : appel `GET subscription`, signal `subscription`, computed `canCancel` (payant + OWNER + non programmé) et `cancellationScheduled`, méthodes `openCancelDialog()` / `confirmCancel()` / `resume()`.
- `CancelSubscriptionDialogComponent` (nouveau, standalone) — dialog de confirmation.
- Service billing frontend — méthodes `getSubscription()`, `cancelSubscription()`, `resumeSubscription()`.
- Le rôle OWNER de l'utilisateur courant est lu depuis la source déjà disponible côté frontend (contexte workspace / `seatsSummary` ou service de rôle existant — à confirmer au dev, sans nouvel endpoint).

### Endpoints consommés

`GET /api/v1/billing/subscription`, `POST /api/v1/billing/cancel`, `POST /api/v1/billing/resume` — fournis par SF-247-01 (contrat figé dans `SF-247-01-backend-resiliation.md`).

### Migration Liquibase

- [x] Non applicable.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `WorkspaceBillingComponent` — section « Résilier » visible si payant + OWNER + non programmé.
- [ ] `WorkspaceBillingComponent` — section masquée si non-OWNER ; masquée si FREE.
- [ ] `WorkspaceBillingComponent` — bandeau « résiliation programmée » + date affichés si `cancelAtPeriodEnd`.
- [ ] `confirmCancel()` appelle le service et bascule l'affichage au succès.
- [ ] `resume()` appelle le service et masque le bandeau au succès.
- [ ] Erreur backend → `MatSnackBar` avec le message.
- [ ] `CancelSubscriptionDialogComponent` — rend le contenu de confirmation, émet le résultat au clic « Confirmer ».

### Isolation workspace

- [x] Non applicable côté frontend — l'isolation est garantie par le backend (SF-247-01).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — aucune nouvelle route ; section ajoutée à un écran existant. Vérifier que l'écran `/workspace/billing` reste fonctionnel.
- [ ] Auth / Principal — non.
- [ ] Workspace context — la section dépend du workspace courant ; le composant se recharge déjà sur changement de workspace (comportement existant).
- [ ] Plans / limites — non (lecture seule côté frontend).

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|---|---|---|
| `WorkspaceBillingComponent` | ajout de blocs — les blocs existants (plans, top-up, seats) ne doivent pas régresser | suite Jest existante `workspace-billing.component.spec.ts` |

### Smoke tests E2E concernés

- [ ] `cd e2e && npm test` — exécuté côté SF-247-01 (préoccupation Plans/limites) ; vérifier que l'écran billing se charge sans régression.

---

## Dépendances

### Subfeatures bloquantes

- `SF-247-01` — contrat d'API figé ; développement parallélisable, intégration finale après merge backend (cf. mémoire `feedback_pre_merge_endpoint_check` — vérifier la présence des endpoints avant merge).

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- Bandeau « résiliation programmée » en haut (visible sans scroll), section « Résilier » en bas et dé-emphasée — ajustements A1/A2 du cadrage écran `SF-247-00b-ux-coherence.md`.
- La section n'apparaît que si plan payant + OWNER — ajustement A3.
