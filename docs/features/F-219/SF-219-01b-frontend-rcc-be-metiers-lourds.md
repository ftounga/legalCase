# Mini-spec — F-219 / SF-219-01b-frontend Outil RCC métiers lourds (UI)

## Identifiant

`F-219 / SF-219-01b-frontend`

## Feature parente

`F-219` — P3 Travail BE — ~32 outils BE-only spécificité

## Statut

`ready`

## Date de création

2026-05-27

## Branche Git

`feat/SF-219-01b-frontend-rcc-be-metiers-lourds`

## Cadrages amont

`SF-219-00-coherence.md` (GO) et `SF-219-00b-ux-coherence.md` (GO avec ajustements) — produits dans cette PR docs.

---

## Objectif

Livrer la section frontend décisionnelle de l'analyseur RCC métiers lourds BE (consommant SF-219-01 backend). Conforme au pattern canonique F-IA-04, exposée **uniquement aux workspaces BELGIQUE / DROIT_DU_TRAVAIL**, visibility **ALWAYS_ON priority 119** (pattern F-213).

---

## Contrat API (figé en SF-219-01 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-metiers-lourds`
- `GET` du même path
- Body : `{ ageFinContrat: number, anneesCarriereTotal: number, anneesMetierLourdRecent10: number, anneesMetierLourdRecent15: number, typeMetierLourd: 'EQUIPES_SUCCESSIVES_NUIT'|'INTERRUPTIONS_HORAIRES_VARIABLES'|'TRAVAIL_PENIBLE'|'AUTRE', licenciementEffectif: boolean }`
- Réponse 200 : `{ verdict: 'ELIGIBLE'|'INELIGIBLE', raisonIneligibilite: string|null, synthese: string, baseJuridique: string, avertissement: string }`

---

## Comportement attendu

### Section composant

`rcc-be-metiers-lourds-section.component` — formulaire + verdict :
- Champs : `ageFinContrat` (number input années), `anneesCarriereTotal` (number), `anneesMetierLourdRecent10` (number), `anneesMetierLourdRecent15` (number), `typeMetierLourd` (select 4 valeurs), `licenciementEffectif` (checkbox).
- Pré-fill IA : limité aux champs déjà extraits par le modèle de base — `ageFinContrat` (dérivé de `dateNaissance` + `dateRuptureContrat` si disponible) — **BELGIQUE UNIQUEMENT**. Les autres champs (carrière totale, métier lourd, type) restent en saisie manuelle avocat V1.
- Badge « Pré-rempli depuis l'analyse » + signal provenance par champ pré-fillable.
- Validation F-IA-03 — `coherenceAlerts` si divergence champ / source IA (sur le seul champ pré-fillable).
- Bouton « Analyser l'éligibilité » → POST → verdict affiché :
  - `ELIGIBLE` : badge vert + synthèse + base juridique.
  - `INELIGIBLE` : badge rouge + raison + base juridique.
- `avertissement` affiché si présent (liste métiers lourds à vérifier).
- `CaseDashboardRefreshService.triggerRefresh()` sur succès POST.
- `MatSnackBar` pour erreurs.
- OnPush + signals + `markForCheck()` dans `next:`/`error:` (mémoire `feedback_onpush_subscribe_markforcheck`).

### Pré-fill rules

| Champ | Source | Règle |
|---|---|---|
| `ageFinContrat` | `aiData.dateNaissance` + `aiData.dateRuptureContrat` | calcul années entre les deux ; null si une des deux absente |
| `anneesCarriereTotal` | non pré-rempli | saisie manuelle V1 |
| `anneesMetierLourdRecent10` | non pré-rempli | saisie manuelle V1 |
| `anneesMetierLourdRecent15` | non pré-rempli | saisie manuelle V1 |
| `typeMetierLourd` | non pré-rempli | saisie manuelle V1 |
| `licenciementEffectif` | `aiData.motifRupture === 'LICENCIEMENT'` si présent | défaut true |

`getPrefillCount(input)` retourne 0-2.

### Entrée TOOL_REGISTRY

- `tool_id` : `rcc-be-metiers-lourds`
- Visibility : `ALWAYS_ON`, `priority = 119`
- Ordre : position 19 dans la séquence TOOL_REGISTRY BE (cf. étape 0 bis)
- THEME_BY_TOOL_ID : thème `legal-decisional-analyzer` (analyseur d'éligibilité)

### Visibility seed (migration backend incluse dans cette SF)

Migration `XXX-add-rcc-be-metiers-lourds-visibility.xml` :
- INSERT `decision_tool_visibility_rules` : `tool_id='rcc-be-metiers-lourds'`, `country='BELGIQUE'`, `legal_domain='DROIT_DU_TRAVAIL'`, `visibility='ALWAYS_ON'`, `priority=119`.

### KNOWN_NO_DASHBOARD_TILE_IDS

**Obligatoire** (mémoire `feedback_f213_backend_pattern` — récidive 2 fois en 24 h sur F-213) :
- Ajouter `"rcc-be-metiers-lourds"` à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS` côté backend.
- Sinon master-red garanti.

### KNOWN_FRONTEND_TOOL_IDS

Ajouter `"rcc-be-metiers-lourds"` à `KNOWN_FRONTEND_TOOL_IDS` (`DecisionToolVisibilityIntegrityIT` côté backend) — sinon panel orphelin (mémoire `feedback_pre_merge_visibility_seed_check`).

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] `<input type="number">` pour les champs numériques
- [x] `JetBrains Mono` pour `baseJuridique`
- [x] Erreurs via `MatSnackBar`
- [x] `CaseDashboardRefreshService.triggerRefresh()` dans `next:` du POST
- [x] Pré-fill IA + signaux provenance + badges (sur les champs pré-fillables)
- [x] Validation F-IA-03 (`coherenceAlerts` + `<app-coherence-popover-trigger>`)
- [x] `getPrefillCount(input)` static, parité avec `prefillFromAi()`
- [x] `tool_id` ajouté à `KNOWN_FRONTEND_TOOL_IDS`
- [x] `tool_id` ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS`
- [x] OnPush + ChangeDetectorRef + `markForCheck()` dans `next/error`

---

## Critères d'acceptation

- [ ] Gate `workspaceCountry === 'BELGIQUE'` strict — outil masqué pour workspace FR.
- [ ] Pré-fill IA actif pour `ageFinContrat` si dates disponibles.
- [ ] Verdict ELIGIBLE → badge vert + synthèse + base juridique.
- [ ] Verdict INELIGIBLE → badge rouge + raison.
- [ ] `avertissement` affiché.
- [ ] `getPrefillCount` retourne le bon compte.
- [ ] `DecisionToolVisibilityIntegrityIT` reste vert.
- [ ] `DashboardTileToolIdIntegrityIT` reste vert.
- [ ] Migration visibility ALWAYS_ON / BELGIQUE / DROIT_DU_TRAVAIL appliquée avec priority 119.

---

## Périmètre

### Hors scope

- Backend — SF-219-01.
- Autres outils F-219.
- Calcul indemnité complémentaire — F-207 SF-207-07.

---

## Plan de test (Jest)

- [ ] `rcc-be-metiers-lourds-section-prefill-rules.spec.ts` — 4 tests : 0/1/2 champs pré-fillés, calcul age depuis dates.
- [ ] `rcc-be-metiers-lourds-section.component.spec.ts` — rendu sans BELGIQUE, pré-fill, verdict ELIGIBLE/INELIGIBLE, refresh, snackbar erreur, OnPush markForCheck.
- [ ] `DecisionToolVisibilityIntegrityIT` (backend) reste vert.
- [ ] `DashboardTileToolIdIntegrityIT` (backend) reste vert.

---

## Technique

### Composants Angular

- `frontend/src/app/case-files/rcc-be-metiers-lourds-section/rcc-be-metiers-lourds-section.component.{ts,html,scss,spec.ts}`
- `frontend/src/app/case-files/rcc-be-metiers-lourds-section/rcc-be-metiers-lourds-section-prefill-rules.{ts,spec.ts}`
- `frontend/src/app/core/models/rcc-be-metiers-lourds.model.ts`
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — entry TOOL_REGISTRY + THEME_BY_TOOL_ID

### Backend (migration visibility + KNOWN_*)

- `backend/src/main/resources/db/changelog/migrations/XXX-add-rcc-be-metiers-lourds-visibility.xml`
- `backend/src/test/java/.../DashboardTileToolIdIntegrityIT.java` — ajout à `KNOWN_NO_DASHBOARD_TILE_IDS`
- `backend/src/test/java/.../DecisionToolVisibilityIntegrityIT.java` — ajout à `KNOWN_FRONTEND_TOOL_IDS`

---

## Dépendances

- SF-219-01 backend — doit être mergée avant le push de cette SF.
