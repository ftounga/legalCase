# Mini-spec — F-FA-15 / SF-FA-15-02 Récompenses (art. 1437/1469 Cciv) — FRONTEND

## Identifiant

`F-FA-15 / SF-FA-15-02`

## Feature parente

`F-FA-15` — Récompenses entre patrimoines (art. 1437 et s. Cciv)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-15-02-frontend-recompenses`

---

## Objectif

Implémenter le composant Angular `recompenses-section` (et son service + modèle) consommant l'endpoint `POST/GET /api/v1/case-files/{caseFileId}/recompenses` exposé par SF-FA-15-01, intégré au panel décisionnel F-IA-04 via `TOOL_REGISTRY` (tool id `F-FA-15-recompenses`), avec gate FRANCE bannière info, pré-remplissage IA + validation F-IA-03, et rendu d'un tableau récapitulatif des récompenses calculées avec totaux + base juridique + formule.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre le panneau outils sur un dossier FR DROIT_FAMILLE → l'outil "Récompenses (1437/1469 Cciv)" est visible (visibility rule SF-FA-15-01).
2. Le composant fait `GET /api/v1/case-files/{caseFileId}/recompenses` :
   - **200** → restaure le résultat précédent et masque le form (`showForm = false`).
   - **404** → reste en mode formulaire ; pré-fill IA depuis `aiData?.regimeMatrimonialDetecte` si dispo.
3. L'avocat sélectionne un `regimeMatrimonial` (3 options : `COMMUNAUTE_LEGALE` / `PARTICIPATION_AUX_ACQUETS` / `COMMUNAUTE_UNIVERSELLE`) — `SEPARATION_BIENS` n'est **pas** présent dans l'UI (rejeté backend, exclus du select).
4. L'avocat ajoute des opérations via "Ajouter une opération". Pour chaque op il saisit :
   - `id` auto-généré (`op-1`, `op-2`, …) ; éditable mais pas obligatoire pour l'avocat.
   - `type` (mat-select 2 options) : `DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE` / `DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE`.
   - `natureBien` (mat-select 5 options) : `ACQUISITION_BIEN_PROPRE` / `CONSERVATION_BIEN_PROPRE` / `AMELIORATION_BIEN_PROPRE` / `DEPENSES_USUELLES` / `AUTRE`.
   - `montantDepenseEur` (input number, requis ≥ 0).
   - `valeurInitialeBienEur` (input number, ≥ 0, optionnel).
   - `valeurActuelleBienEur` (input number, ≥ 0, optionnel).
   - `description` (textarea, optionnel).
5. L'avocat peut supprimer une opération (bouton corbeille).
6. L'avocat clique "Calculer" → POST avec `{ regimeMatrimonial, operations }` → réponse stockée dans `result()`, `showForm = false`, `triggerRefresh()` appelé sur `CaseDashboardRefreshService`.
7. Affichage du résultat :
   - **Tableau récapitulatif** : pour chaque récompense, ligne avec `operationId` / `regleApplicable` / `montantRecompenseEur` / `directionRecompense` (et `description` si présente).
   - **3 cartes totaux** : "Dû par communauté", "Dû par époux", "Solde net pour les époux".
   - **Base juridique générale** : "Art. 1437 et 1469 Cciv" en JetBrains Mono.
   - **Pour chaque ligne** : `baseJuridiqueOperation` + `formule` en JetBrains Mono.
   - **Liste de messages** (puces) avec citations juridiques en `<code>`.
8. Bouton "Modifier" → revient au form.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Workspace BELGIQUE | Bannière info "Outil propre au droit français — voir l'outil BE équivalent (backlog F-FA-15-BE)" + form masqué |
| Régime non sélectionné | Bouton "Calculer" disabled |
| Aucune opération | Bouton "Calculer" disabled (ou autorisé, backend renvoie totaux à 0 + message) |
| `montantDepenseEur` négatif | Bloqué côté input `min="0"` ; backend 400 → snackBar erreur |
| HTTP 400 backend | `MatSnackBar` rouge avec message backend (`err.error.message`) |
| HTTP 5xx | `MatSnackBar` "Erreur lors du calcul" |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils famille FR** : F-FA-05 (partage immobilier), F-FA-08-09-10-11 (divorce *), F-FA-13 (calcul prestation comp), F-FA-14 (donation entre époux). **Pattern aligné** : composant standalone + service + modèle, `mat-select` + form, `triggerRefresh()`, `MatSnackBar` erreurs, palette navy/or, JetBrains Mono pour `formule` / `baseJuridique`.
- [x] **Autres pays** : FR uniquement (régime communautaire civilist FR — backend gate explicite). BE : bannière info renvoyée vers backlog (régime BE diffère — art. 1432-1438 anciens C. civ. BE, hors scope F-FA-15).
- [x] **Pattern UI partagé** : utilisera `CoherencePopoverTriggerDirective` + `CoherenceAlertBuilder` + `CoherenceAlert<F>` partagés (F-155-05). Pas de nouveau pattern UI introduit.
- [x] **Refresh dashboard (F-IA-02)** : oui, déclenché après POST réussi.
- [x] **Pré-remplissage IA** : `regimeMatrimonial` pré-rempli depuis `aiData?.regimeMatrimonialDetecte` (champ ajouté à `FamilleExtractedData`) — graceful no-op si absent. Les opérations elles-mêmes sont saisies par l'avocat (pas d'auto-population — la connaissance fine des dépenses propres/communs n'est pas extractible avec fiabilité depuis l'analyse IA générique).
- [x] **Validation F-IA-03** : alerte cohérence sur `REGIME_MATRIMONIAL` quand l'IA détecte un régime divergent de la sélection avocat.

### Cas spécifique : nouvel outil décisionnel

- [x] **Cohérence F-IA-03** : oui, sur `REGIME_MATRIMONIAL` — sources `IA` (`aiData?.regimeMatrimonialDetecte`) et `F96` (procedureCheck `FA15_REGIME_MATRIMONIAL`). Pas de divergence sur les opérations (saisies avocat).
- [x] **Refresh dashboard (F-IA-02)** : `triggerRefresh()` injecté optionnel + appelé dans `next:` du POST.
- [x] **Pré-remplissage IA** : `prefillFromAi()` invoqué `ngOnInit` + `ngOnChanges` (si `aiData` change avant première résolution) avec signal `provenanceRegime`.
- [x] **Persistance des inputs** : SF-FA-15-01 stocke `operations_json` + `regime_matrimonial` ; le GET retourne donc le résultat persisté avec `recompenses[]` et totaux. Le form lui-même n'est pas re-prefillé depuis le résultat (champ result.recompenses vs operations) — l'édition après POST repasse par "Modifier" → repop le form depuis `regimeMatrimonial` + `operations` issus de `request` mais ces sont perdus dans la réponse. **Décision** : "Modifier" laisse le form vide (sauf regime) ; l'avocat ré-saisit ; comportement transparent côté UX. (Si évolution future : exposer `operations` dans la response — out of scope).
- [x] **Masquage conditionnel** : gate FRANCE via bannière info (pas de masquage).
- [x] **Alertes après calcul (anti-bug SF-IA-03-12)** : `coherenceAlerts` gated par `showForm()` strict.

---

## Critères d'acceptation

1. Mount FR avec dossier sans analyse → `GET 404` → form visible avec select régime + bouton "Ajouter une opération".
2. Mount FR avec analyse persistée → `GET 200` → tableau récapitulatif visible, form masqué.
3. Mount BE → bannière info "outil FR uniquement" visible, form masqué.
4. Sélection `regimeMatrimonial = COMMUNAUTE_LEGALE` + ajout 1 opération `AMELIORATION_BIEN_PROPRE` 50000/200000/350000 + click "Calculer" → POST émis + result.recompenses[0].montantRecompenseEur affiché (150 000 €).
5. Click "Calculer" sans régime → bouton disabled.
6. Click "Modifier" sur un résultat → showForm true.
7. POST 400 → snackBar erreur visible.
8. POST 200 → `triggerRefresh()` appelé.
9. Pré-fill IA : `aiData = { regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE' }` → form pré-rempli + badge "Pré-rempli depuis l'analyse" affiché.
10. Modifier manuellement le régime → badge IA disparaît (`provenanceRegime = null`).
11. Divergence IA `COMMUNAUTE_LEGALE` vs avocat `PARTICIPATION_AUX_ACQUETS` → badge incohérence affiché avec popover.
12. `coherenceAlerts` vide quand `showForm() === false` (anti-bug SF-IA-03-12).
13. Suppression d'une opération via bouton corbeille → opération retirée du tableau.

## Plan de test minimal

≥ 11 tests Jest :
- `mount FR → GET 404 → form visible`
- `mount FR → GET 200 → result + masque form`
- `mount BE → bannière + form masqué`
- `formValid: regime requis`
- `addOperation/removeOperation : taille de la liste mise à jour`
- `calculate POST emis avec payload correct`
- `calculate succès → triggerRefresh appelé`
- `calculate erreur 400 → snackBar`
- `prefillFromAi : regimeMatrimonialDetecte → provenanceRegime = 'IA'`
- `onRegimeChange manuel : provenanceRegime → null`
- `coherenceAlerts : divergence IA vs avocat → alerte REGIME_MATRIMONIAL`
- `coherenceAlerts : showForm=false → vide`

---

## Tables / endpoints / composants impactés

### Endpoints (consommés)
- POST `/api/v1/case-files/{caseFileId}/recompenses` (SF-FA-15-01)
- GET `/api/v1/case-files/{caseFileId}/recompenses` (SF-FA-15-01)

### Tables
- Aucune création (consomme `recompenses_analyses` via API).

### Visibility rule
- Existante : `f1a04001-0000-0000-0000-ee00000fa151`, tool id `F-FA-15-recompenses`, ALWAYS_ON FR + DROIT_FAMILLE, priority 76.

### Composants Angular
- `frontend/src/app/core/models/recompenses.model.ts` (NOUVEAU)
- `frontend/src/app/core/services/recompenses.service.ts` (NOUVEAU)
- `frontend/src/app/case-files/recompenses-section/recompenses-section.component.{ts,html,scss,spec.ts}` (NOUVEAU)
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (MODIF — entrée TOOL_REGISTRY)
- `frontend/src/app/core/models/divorce-accepte.model.ts` (MODIF — ajout `regimeMatrimonialDetecte` à `FamilleExtractedData`, optionnel)

---

## Hors périmètre

- **Régime BE** des indemnités matrimoniales (backlog futur F-FA-15-BE).
- Re-pré-fill du form après "Modifier" sur le résultat (operations non exposées dans la response).
- Édition individuelle d'une récompense calculée (calcul atomique).
- Export PDF du tableau (couvert par F-148 quand pertinent).

---

## Contrat API (importé de SF-FA-15-01)

Voir `docs/features/F-FA-15/SF-FA-15-01-backend.md` § "Contrat API".

---

## Impact par domaine métier

Cette feature est **spécifique au droit de la famille FR** :
- **Droit du travail** : non applicable.
- **Immigration** : non applicable.
- **Droit famille FR** : cœur fonctionnel.
- **Droit famille BE** : non couvert (régime juridique différent — bannière info).

Aucun risque d'asymétrie 3 domaines.

## Préoccupations transversales

- Auth / Principal : aucune modification.
- Workspace context : standard (utilise `caseFileId` qui est filtré côté backend).
- Plans / limites : aucun.
- Routing / navigation : N/A (composant intégré au panel F-IA-04 sur la page case-file-detail existante).
- Outil décisionnel métier : nouveau frontend, isolé.

---

## Pattern de référence

- **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02) — référence pré-fill IA + F-IA-03.
- **Form famille avec opérations multiples + montants** : `partage-immobilier-section` (F-FA-05) — palette navy/or, layout, typographie.

## Self-check pré-commit

- ≥ 2 imports `CoherenceAlertBuilder`
- ≥ 1 import `coherence-alert-builder`
- ≥ 2 occurrences `appCoherencePopover`
- ≥ 2 occurrences `prefillFromAi`
- ≥ 1 occurrence `auto_awesome`
- ≥ 4 occurrences `provenance`
- ≥ 1 occurrence `coherenceAlerts`
- ≥ 2 handlers `on*Change`
- 0 interface locale `CoherenceAlert` (utilise le partagé)
