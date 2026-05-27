# Mini-spec — F-219 / SF-219-32b-frontend Outil interruption carriere soins parental (UI)

## Identifiant

`F-219 / SF-219-32b-frontend`

## Feature parente

`F-219` — P3 Travail BE — ~32 outils BE-only spécificité

## Statut

`ready`

## Date de création

2026-05-27

## Branche Git

`feat/SF-219-32b-frontend-interruption-carriere-soins-parental`

## Cadrages amont

`SF-219-00-coherence.md` (GO) et `SF-219-00b-ux-coherence.md` (GO avec ajustements) — produits dans cette PR docs.

---

## Objectif

Livrer la section frontend décisionnelle de l'outil **interruption carriere soins parental** BE (consommant SF-219-32 backend). Conforme au pattern canonique F-IA-04, exposée **uniquement aux workspaces BELGIQUE / DROIT_DU_TRAVAIL**, visibility **ALWAYS_ON priority 150** (pattern F-213).

---

## Contrat API (figé en SF-219-32 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/interruption-carriere-soins-parental`
- `GET` du même path
- Body : voir SF-219-32-backend (inputs métier spécifiques).
- Réponse 200 : `{ verdict, raison|null, synthese, baseJuridique, avertissement }`

---

## Comportement attendu

### Section composant

`interruption-carriere-soins-parental-section.component` — formulaire + verdict :
- Champs : selon SF-219-32-backend (formulaire reactive Forms, inputs typés).
- Pré-fill IA : limité aux champs déjà extraits par le modèle de base (ex. `salaireBrutAnnuel`, `dateRuptureContrat`, `ancienneteAnnees`, `motifRupture` selon pertinence outil) — **BELGIQUE UNIQUEMENT**. Champs métier spécifiques restent en saisie manuelle avocat V1.
- Badge « Pré-rempli depuis l'analyse » + signal provenance par champ pré-fillable.
- Validation F-IA-03 — `coherenceAlerts` si divergence champ / source IA (sur les champs pré-fillables).
- Bouton « Analyser » → POST → verdict affiché :
  - Verdict positif : badge vert + synthèse + base juridique.
  - Verdict négatif : badge rouge + raison + base juridique.
- `avertissement` affiché si présent.
- `CaseDashboardRefreshService.triggerRefresh()` sur succès POST.
- `MatSnackBar` pour erreurs.
- OnPush + signals + `markForCheck()` dans `next:`/`error:` (mémoire `feedback_onpush_subscribe_markforcheck`).

### Pré-fill rules

Limité aux champs cross-domain déjà extraits par le modèle BE de base. Détail dans `interruption-carriere-soins-parental-section-prefill-rules.ts`. `getPrefillCount(input)` retourne le nombre de champs pré-fillés.

### Entrée TOOL_REGISTRY

- `tool_id` : `interruption-carriere-soins-parental`
- Visibility : `ALWAYS_ON`, `priority = 150`
- Ordre : position 50 dans la séquence TOOL_REGISTRY BE (cf. étape 0 bis)
- THEME_BY_TOOL_ID : thème adapté au type d'outil (legal-decisional-calculator)

### Visibility seed (migration backend incluse dans cette SF)

Migration `XXX-add-interruption-carriere-soins-parental-visibility.xml` :
- INSERT `decision_tool_visibility_rules` : `tool_id='interruption-carriere-soins-parental'`, `country='BELGIQUE'`, `legal_domain='DROIT_DU_TRAVAIL'`, `visibility='ALWAYS_ON'`, `priority=150`.

### KNOWN_NO_DASHBOARD_TILE_IDS

**Obligatoire** (mémoire `feedback_f213_backend_pattern` — récidive 2 fois en 24 h sur F-213) :
- Ajouter `"interruption-carriere-soins-parental"` à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS` côté backend.
- Sinon master-red garanti.

### KNOWN_FRONTEND_TOOL_IDS

Ajouter `"interruption-carriere-soins-parental"` à `KNOWN_FRONTEND_TOOL_IDS` (`DecisionToolVisibilityIntegrityIT` côté backend) — sinon panel orphelin (mémoire `feedback_pre_merge_visibility_seed_check`).

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] `<input type="number">` pour les champs numériques (si applicable)
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
- [ ] Pré-fill IA actif pour les champs cross-domain pré-fillables.
- [ ] Verdict positif → badge vert + synthèse + base juridique.
- [ ] Verdict négatif → badge rouge + raison.
- [ ] `avertissement` affiché si présent.
- [ ] `getPrefillCount` retourne le bon compte.
- [ ] `DecisionToolVisibilityIntegrityIT` reste vert.
- [ ] `DashboardTileToolIdIntegrityIT` reste vert.
- [ ] Migration visibility ALWAYS_ON / BELGIQUE / DROIT_DU_TRAVAIL appliquée avec priority 150.

---

## Périmètre

### Hors scope

- Backend — SF-219-32.
- Autres outils F-219.

---

## Plan de test (Jest)

- [ ] `interruption-carriere-soins-parental-section-prefill-rules.spec.ts` — tests pré-fill (0 / N champs, mapping valeurs).
- [ ] `interruption-carriere-soins-parental-section.component.spec.ts` — rendu sans BELGIQUE, pré-fill, verdict positif/négatif, refresh, snackbar erreur, OnPush markForCheck.
- [ ] `DecisionToolVisibilityIntegrityIT` (backend) reste vert.
- [ ] `DashboardTileToolIdIntegrityIT` (backend) reste vert.

---

## Technique

### Composants Angular

- `frontend/src/app/case-files/interruption-carriere-soins-parental-section/interruption-carriere-soins-parental-section.component.{ts,html,scss,spec.ts}`
- `frontend/src/app/case-files/interruption-carriere-soins-parental-section/interruption-carriere-soins-parental-section-prefill-rules.{ts,spec.ts}`
- `frontend/src/app/core/models/interruption-carriere-soins-parental.model.ts`
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — entry TOOL_REGISTRY + THEME_BY_TOOL_ID

### Backend (migration visibility + KNOWN_*)

- `backend/src/main/resources/db/changelog/migrations/XXX-add-interruption-carriere-soins-parental-visibility.xml`
- `backend/src/test/java/.../DashboardTileToolIdIntegrityIT.java` — ajout à `KNOWN_NO_DASHBOARD_TILE_IDS`
- `backend/src/test/java/.../DecisionToolVisibilityIntegrityIT.java` — ajout à `KNOWN_FRONTEND_TOOL_IDS`

---

## Dépendances

- SF-219-32 backend — doit être mergée avant le push de cette SF.
