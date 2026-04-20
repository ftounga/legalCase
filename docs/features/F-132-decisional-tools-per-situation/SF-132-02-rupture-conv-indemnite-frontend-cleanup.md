# Mini-spec — F-132 / SF-132-02 Frontend "Indemnité rupture conventionnelle" FR + cleanup backend

## Identifiant
`F-132 / SF-132-02`

## Feature parente
`F-132` — Refonte F-DT-09 en outils décisionnels dédiés

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-132-02-rupture-conv-indemnite-frontend-cleanup`

---

## Objectif

Exposer côté frontend l'outil backend livré en SF-132-01 (`rupture-conv-indemnite-section`), l'afficher conditionnellement sur les dossiers FR de rupture conventionnelle, masquer F-DT-09 dans ce contexte, et retirer la branche `RUPTURE_CONVENTIONNELLE` de `IndemniteComparatifCalculator` (cleanup). Corriger au passage la card dashboard "Indemnités estimées" pour afficher la vraie indemnité minimum sur les dossiers rupture conventionnelle (fix UX du bug E28).

---

## Comportement attendu

### Cas nominal

Sur un dossier `DROIT_DU_TRAVAIL` + `workspaceCountry == FRANCE` + `synthesis.compensationEstimate.typeRupture == RUPTURE_CONVENTIONNELLE` :
- `app-indemnite-comparatif-section` est **masquée**
- `app-rupture-conv-indemnite-section` est **affichée** avec formulaire à 2 champs : `ancienneteAnnees` + `salaireMensuel`
- Pré-remplissage IA : `compensationEstimate.ancienneteAnnees` et `compensationEstimate.salaireMensuel` via `prefillFromAi` au premier chargement si présents
- Bouton "Calculer" → `POST /api/v1/case-files/{id}/rupture-conv-indemnite` → affichage de `indemniteLegaleMinimum`, `formule`, `baseJuridique`, `messages`
- Card dashboard "Indemnités estimées" affiche **le minimum légal calculé** (pas 0 — 0 €)

Sur un dossier FR licenciement / Belgique / autre : comportement F-DT-09 inchangé.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Inputs invalides côté form | Bouton "Calculer" désactivé + erreurs `mat-error` |
| Backend 400 | Snackbar rouge avec le message backend |
| Backend 500 | Snackbar rouge "Erreur technique, réessayer" |
| GET sans analyse préalable | Affichage du formulaire vide (pas d'erreur UI) |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|---|---|---|
| Outils décisionnels jumeaux (Belgique CCT 109 / rupture amiable) | Oui | **SF-132-03** |
| `RecoursGenerator` | Oui | **F-133** |
| Autres outils FR/BE (F-DT-07/08/10, F-IM, F-FA) | Non applicable | Scan F-132 déjà effectué, pas de multi-situations |
| Cohérence IA (F-IA-03) | Non — outil purement calculatoire (2 inputs → 1 sortie), pas de critère à croiser avec l'IA | N/A |
| Refresh dashboard (F-IA-02) | Oui — la card "Indemnités estimées" doit refléter la nouvelle source | **Intégré** : après POST, `CaseDashboardRefreshService.triggerRefresh()` dans `next:` (pattern SF-IA-02-03). Backend `CaseFileDashboardService.buildIndemniteSummary` adapté pour lire `RuptureConvIndemniteAnalysis` en priorité si `type_rupture == RUPTURE_CONVENTIONNELLE` |
| Pré-remplissage IA | Oui | **Intégré** : `prefillFromAi()` lit `synthesis.compensationEstimate.ancienneteAnnees` et `compensationEstimate.salaireMensuel` |
| Persistance inputs | Oui — déjà traitée côté backend en SF-132-01 (colonnes dédiées) | Déjà fait |
| Masquage conditionnel selon type | Oui — cœur de la SF | **Intégré** : nouveau computed `showRuptureConvIndemnite` dans `case-file-detail`, F-DT-09 masqué quand ce computed est vrai |
| Nouveau pattern UI partagé | Non — réutilise les patterns existants (formulaires réactifs, snackbar, `CaseDashboardRefreshService`) | N/A |

### Décision

- [x] Étendu aux cibles applicables (refresh dashboard, masquage conditionnel, pré-remplissage)
- [x] SFs parallèles créées : SF-132-03 (Belgique), F-133 (RecoursGenerator)
- [x] Non applicable aux autres outils (scan F-132)

---

## Critères d'acceptation

### Frontend

- [ ] Nouveau composant `rupture-conv-indemnite-section.component.ts` (+ html + scss + spec) dans `frontend/src/app/case-files/rupture-conv-indemnite-section/`
- [ ] Nouveau service `rupture-conv-indemnite.service.ts` dans `frontend/src/app/core/services/`
- [ ] Nouveau modèle `rupture-conv-indemnite.model.ts` dans `frontend/src/app/core/models/` (Request, Response, Result)
- [ ] Formulaire avec 2 champs : `ancienneteAnnees` (number ≥ 0) et `salaireMensuel` (number > 0), `mat-form-field` appearance `outline`, `mat-error` pour les erreurs
- [ ] Bouton "Calculer" désactivé si formulaire invalide, spinner pendant le call, snackbar succès/erreur
- [ ] Affichage du résultat : indemnité en gros, formule en sous-texte, baseJuridique, messages (2 bullets)
- [ ] Pré-remplissage IA depuis `synthesis.compensationEstimate.ancienneteAnnees/salaireMensuel` au premier chargement
- [ ] `case-file-detail` : nouveau computed `showRuptureConvIndemnite` = `legalDomain=DROIT_DU_TRAVAIL && country=FRANCE && compensationEstimate?.typeRupture==RUPTURE_CONVENTIONNELLE`
- [ ] `case-file-detail` : `app-indemnite-comparatif-section` affichée seulement si `!showRuptureConvIndemnite()` (masquage)
- [ ] `case-file-detail` : `app-rupture-conv-indemnite-section` affichée si `showRuptureConvIndemnite()`
- [ ] Couleurs/polices conformes `DESIGN_SYSTEM.md`, espacements 4px, `MatSnackBar` pour notifications
- [ ] Au moins 5 tests Jest : form valide → submit OK, form invalide → submit désactivé, prefill IA, masquage, error snackbar

### Backend

- [ ] Retirer `"RUPTURE_CONVENTIONNELLE"` de `IndemniteComparatifCalculator.TYPES_RUPTURE_FR` (restreint à licenciement)
- [ ] Supprimer la branche `if ("RUPTURE_CONVENTIONNELLE".equals(typeRupture))` dans `calculateFrance()` (lignes 49-64). Le calculator ne gère plus que le Macron pour la France.
- [ ] Supprimer la méthode privée `computeIndemniteLegaleLicenciement` (utilisée uniquement par la branche supprimée ; déjà répliquée dans `RuptureConvIndemniteCalculator` SF-132-01)
- [ ] Mettre à jour `IndemniteComparatifCalculatorTest` : retirer les tests qui passaient `RUPTURE_CONVENTIONNELLE` (ils devront désormais lever `IllegalArgumentException`)
- [ ] Mettre à jour `IndemniteComparatifControllerIT` : retirer (ou adapter en attendant 400) les tests IT `RUPTURE_CONVENTIONNELLE`
- [ ] `CaseFileDashboardService.buildIndemniteSummary` : si `type_rupture == RUPTURE_CONVENTIONNELLE`, lire `RuptureConvIndemniteAnalysis` et renvoyer une `IndemniteSummary` avec `fourchetteBasse == fourhetteHaute == indemniteLegaleMinimum` et `baremeSource == "Indemnité légale de licenciement (art. R1234-2)"` ; sinon comportement existant (F-DT-09)
- [ ] Tous les tests backend restent verts

---

## Périmètre

### Hors scope

- Belgique (CCT 109 vs rupture amiable) → **SF-132-03**
- `RecoursGenerator` F-IM-06 → **F-133**
- Migration de données `IndemniteComparatif` existants marqués rupture conv → **non nécessaire** : l'ancien endpoint refusera désormais les rupture conv (400) ; les avocats devront resaisir via le nouvel outil (données de test staging seulement, pas de prod encore)
- Intégration F-IA-03 (pas de réponse avocat libre sur cet outil)
- Export PDF du résultat rupture conv — peut être ajouté en follow-up si demandé

---

## Contraintes de validation

### Frontend (form)

| Champ | Obligatoire | Format | Validator |
|---|---|---|---|
| `ancienneteAnnees` | Oui | entier ≥ 0 | `Validators.required`, `Validators.min(0)` |
| `salaireMensuel` | Oui | number > 0 | `Validators.required`, `Validators.min(0.01)` |

### Backend (inchangé SF-132-01)

---

## Technique

### Endpoints consommés

- `POST /api/v1/case-files/{id}/rupture-conv-indemnite` (déjà livré SF-132-01)
- `GET /api/v1/case-files/{id}/rupture-conv-indemnite` (idem)

### Tables impactées

- `rupture_conv_indemnite_analyses` : lecture dans `CaseFileDashboardService.buildIndemniteSummary` pour alimenter la card dashboard quand rupture conv
- `indemnite_comparatifs` : inchangée (cohabitation ; rupture conv ne sera plus stockée ici)

### Migration Liquibase

- [ ] Aucune

### Composants Angular

- `RuptureConvIndemniteSectionComponent` — formulaire + résultat + snackbar + prefill IA
- `CaseFileDetailComponent` — ajout `showRuptureConvIndemnite` computed + masquage conditionnel `app-indemnite-comparatif-section`

---

## Plan de test

### Tests Jest frontend

- `RuptureConvIndemniteSectionComponent` :
  - Form invalide → bouton "Calculer" disabled
  - Form valide → submit appelle le service + affiche le résultat
  - Prefill IA : `compensationEstimate` non null → champs pré-remplis
  - Erreur backend → snackbar rouge
  - GET vide → formulaire visible sans erreur
- `CaseFileDetailComponent` (tests existants étendus) :
  - `showRuptureConvIndemnite` : vrai seulement si DROIT_DU_TRAVAIL + FRANCE + type=RUPTURE_CONVENTIONNELLE
  - Masquage d'`app-indemnite-comparatif-section` quand `showRuptureConvIndemnite == true`

### Tests backend mis à jour

- `IndemniteComparatifCalculatorTest` : tests `RUPTURE_CONVENTIONNELLE` retirés ou mutés en attente de `IllegalArgumentException`
- `IndemniteComparatifControllerIT` : idem (attente `400` si rupture conv, ou retrait)
- `CaseFileDashboardServiceTest` (si existe) : nouveau test — sur un dossier rupture conv avec `RuptureConvIndemniteAnalysis` persistée, la card renvoie le montant légal minimum

### Isolation workspace

- N/A côté frontend — déjà testée en SF-132-01 côté backend

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal : non
- [ ] Workspace context : non
- [ ] Plans / limites : non
- [x] **Navigation / routing** : `case-file-detail` rend conditionnellement un nouveau composant → smoke tests E2E `workspace.spec.ts` et `navigation.spec.ts` doivent rester verts
- [x] **Outil décisionnel métier** : scan F-132 complet, cibles jumelles tracées (SF-132-03, F-133)
- [x] **Refresh dashboard (F-IA-02)** : la card "Indemnités estimées" lit désormais deux sources selon `type_rupture`, nouveau test requis sur `CaseFileDashboardService`

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `IndemniteComparatifCalculator` / Service / Controller | Branche rupture conv retirée → ne traite plus que licenciement. Consommateurs existants doivent envoyer un type valide. | Tests existants verts (certains supprimés car obsolètes) |
| `CaseFileDashboardService.buildIndemniteSummary` | Dispatch selon `type_rupture` (rupture conv → nouvelle source) | Nouveau test dashboard dédié |
| `app-indemnite-comparatif-section` (HTML) | Masqué conditionnellement dans `case-file-detail` | Snapshot / test de computed |

### Smoke tests E2E concernés

- `e2e/smoke/navigation.spec.ts` — doit rester vert (`case-file-detail` charge sans erreur)
- `e2e/smoke/workspace.spec.ts` — doit rester vert

---

## Dépendances

### Subfeatures bloquantes

- **SF-132-01** mergée (PR #414, 2026-04-20) — backend disponible ✅

### Questions ouvertes

- Aucune

---

## Notes et décisions

- **Migration des données rupture conv existantes dans `indemnite_comparatifs`** : non traitée. Raison : en staging, les rares dossiers de test affectés (dossier E28) pourront simplement être resaisis via le nouvel outil. Pas de prod impactée. Si jamais la prod a des données legacy, prévoir une SF-follow-up ciblée.
- **Pourquoi le retrait du backend dans cette même SF et pas en SF-132-04** : le cleanup est le corollaire direct du frontend qui bascule — les garder ensemble évite que le code mort du backend ne traîne pendant plusieurs PRs. Taille totale de la SF reste ≤ 2 jours.
- **Card dashboard** : j'ajoute le fix ici au lieu de le séparer. Raison : c'est le fix du bug E28 qui a déclenché F-132 — il serait incohérent de laisser la card afficher 0—0 € en attendant SF-132-03 ou plus.
