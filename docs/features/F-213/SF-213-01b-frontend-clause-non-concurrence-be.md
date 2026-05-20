# Mini-spec — F-213 / SF-213-01b-frontend Outil clause non-concurrence BE (UI)

## Identifiant

`F-213 / SF-213-01b-frontend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-01b-frontend-clause-non-concurrence-be`

## Cadrages amont

`SF-213-00-coherence.md` (GO) et `SF-213-00b-ux-coherence.md` (GO avec ajustements) — produits dans cette PR docs.

---

## Objectif

Livrer la section frontend décisionnelle de l'analyseur de clause non-concurrence BE (consommant SF-213-01 backend). Conforme au pattern canonique F-IA-04, exposée **uniquement aux workspaces BELGIQUE / DROIT_DU_TRAVAIL**, visibility **CONTEXTUAL** (`clause_non_concurrence_presente=true`).

---

## Contrat API (figé en SF-213-01 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/clause-non-concurrence-be`
- `GET` du même path
- Body : `{ remunerationAnnuelleBrute: number, dureeMois: number, zoneGeographique: enum, activiteInternationaleProuvee?: boolean, salaireAnnuelSeuil?: number }`
- Réponse 200 : `{ verdict: 'VALIDE'|'NULLE'|'PARTIELLEMENT_NULLE', raisonNullite: string|null, indemniteLegale: number, indemniteLegaleFormule: string, baseJuridique: string, avertissement: string }`

---

## Comportement attendu

### Section composant

`clause-non-concurrence-be-section.component` — formulaire + verdict :
- Champs : `remunerationAnnuelleBrute` (number input €), `dureeMois` (number 1-12), `zoneGeographique` (select), `activiteInternationaleProuvee` (checkbox).
- Pré-fill IA : `prefillFromAi()` depuis `aiData.salaireBrutAnnuel`, `aiData.clauseNonConcurrenceDureeMois`, `aiData.clauseNonConcurrenceZone` — **BELGIQUE UNIQUEMENT**.
- Badge « Pré-rempli depuis l'analyse » + signal provenance par champ.
- Validation F-IA-03 — `coherenceAlerts` si divergence champ / source IA.
- Bouton « Analyser la clause » → POST → verdict affiché :
  - `VALIDE` : badge vert + montant indemnité légale + formule + base juridique.
  - `NULLE` : badge rouge + raison nullité.
  - `PARTIELLEMENT_NULLE` : badge ambre + explication zone.
- `avertissement` affiché si présent (indexation annuelle du seuil).
- `CaseDashboardRefreshService.triggerRefresh()` sur succès POST.
- `MatSnackBar` pour erreurs.

### Pré-fill rules

| Champ | Source | Règle |
|---|---|---|
| `remunerationAnnuelleBrute` | `aiData.salaireBrutAnnuel` | `positiveNumberOrNull()` |
| `dureeMois` | `aiData.clauseNonConcurrenceDureeMois` | `intInRangeOrNull(1,12)` |
| `zoneGeographique` | `aiData.clauseNonConcurrenceZone` | mapping string → enum |
| `activiteInternationaleProuvee` | non pré-rempli | toujours false par défaut |

`getPrefillCount(input)` retourne 0-3.

### Entrée TOOL_REGISTRY

- `tool_id` : `clause-non-concurrence-be`
- Visibility : `CONTEXTUAL`, `trigger_field = 'clause_non_concurrence_presente'`, `trigger_value = 'true'`
- Ordre : position 15 dans la séquence TOOL_REGISTRY BE (cf. étape 0 bis)

### Visibility seed (migration backend incluse dans cette SF)

Migration `XXX-add-clause-non-concurrence-be-visibility.xml` :
- INSERT `decision_tool_visibility_rules` : `tool_id='clause-non-concurrence-be'`, `country='BELGIQUE'`, `legal_domain='DROIT_DU_TRAVAIL'`, `visibility='CONTEXTUAL'`, `trigger_field='clause_non_concurrence_presente'`, `trigger_value='true'`.

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] `<input type="number">` pour rémunération et durée
- [x] `JetBrains Mono` pour `indemniteLegaleFormule` et `baseJuridique`
- [x] Erreurs via `MatSnackBar`
- [x] `CaseDashboardRefreshService.triggerRefresh()` dans `next:` du POST
- [x] Pré-fill IA + signaux provenance + badges
- [x] Validation F-IA-03 (`coherenceAlerts` + `<app-coherence-popover-trigger>`)
- [x] `getPrefillCount(input)` static, parité avec `prefillFromAi()`
- [x] `tool_id` ajouté à `KNOWN_FRONTEND_TOOL_IDS`

---

## Critères d'acceptation

- [ ] Gate `workspaceCountry === 'BELGIQUE'` strict — outil masqué pour workspace FR.
- [ ] Pré-fill IA actif pour rémunération, durée et zone si données disponibles.
- [ ] Verdict VALIDE → badge vert + indemnité légale + formule + base juridique.
- [ ] Verdict NULLE → badge rouge + raison.
- [ ] `avertissement` seuil affiché.
- [ ] `getPrefillCount` retourne le bon compte.
- [ ] `DecisionToolVisibilityIntegrityIT` reste vert.
- [ ] Migration visibility CONTEXTUAL / BELGIQUE / DROIT_DU_TRAVAIL appliquée.

---

## Périmètre

### Hors scope

- Backend — SF-213-01.
- Autres outils F-213.

---

## Plan de test (Jest)

- [ ] `clause-non-concurrence-be-section-prefill-rules.spec.ts` — 5 tests : 0/1/3 champs, mapping zone, rémunération hors range.
- [ ] `clause-non-concurrence-be-section.component.spec.ts` — rendu sans BELGIQUE, pré-fill, verdict VALIDE/NULLE, refresh, snackbar erreur.
- [ ] `DecisionToolVisibilityIntegrityIT` (backend) reste vert.

---

## Technique

### Composants Angular

- `frontend/src/app/case-files/clause-non-concurrence-be-section/clause-non-concurrence-be-section.component.{ts,html,scss,spec.ts}`
- `frontend/src/app/case-files/clause-non-concurrence-be-section/clause-non-concurrence-be-section-prefill-rules.{ts,spec.ts}`
- `frontend/src/app/core/models/clause-non-concurrence-be.model.ts`

### Backend (migration visibility uniquement)

- `backend/src/main/resources/db/changelog/migrations/XXX-add-clause-non-concurrence-be-visibility.xml`

---

## Dépendances

- SF-213-01 backend — doit être mergée avant le push de cette SF.
