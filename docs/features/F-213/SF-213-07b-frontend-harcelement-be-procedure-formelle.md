# Mini-spec — F-213 / SF-213-07b-frontend Outil harcèlement BE procédure formelle (UI)

## Identifiant

`F-213 / SF-213-07b-frontend`

## Feature parente

`F-213` — P2 Travail BE

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Section frontend de la checklist procédure formelle harcèlement BE. Visibility **CONTEXTUAL** (`harcelement_detecte=true`). **BELGIQUE / DROIT_DU_TRAVAIL uniquement.** BE-only.

---

## Contrat API (SF-213-07 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/harcelement-be-procedure-formelle`
- `GET` même path
- Réponse 200 : `{ etapeProcedure, checklistItems, representaillesPossibles, dateDebutProtectionRepresailles, dateFinProtectionRepresailles, prochainDelaiFatal, baseJuridique, avertissement }`

---

## Comportement attendu

`harcelement-be-procedure-formelle-section.component` :
- Champs : `typeHarcelement` (select), `etapeProcedure` (select), `dateDepotPlainte` (date, conditionnel), `entreprisePossedeCPAP` (checkbox), `entrepriseTaille` (select), `mesureDefavorableApres` (checkbox).
- Pré-fill IA : typeHarcelement, dateDepotPlainte, mesureDefavorableApres.
- Résultat : checklist items avec statuts (EN_COURS / ACTIF / A_FAIRE) + `prochainDelaiFatal` en rouge si ≤ 30 j + période de protection représailles (avec badge rouge si `representaillesPossibles=true`).
- `CaseDashboardRefreshService.triggerRefresh()` sur POST.

### Visibility seed

`XXX-add-harcelement-be-procedure-formelle-visibility.xml` :
- `CONTEXTUAL`, `trigger_field='harcelement_detecte'`, `trigger_value='true'`, `BELGIQUE`, `DROIT_DU_TRAVAIL`.

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] `<input type="date">` pour dateDepotPlainte
- [x] Badge rouge si représailles possibles
- [x] `MatSnackBar` erreurs
- [x] Pré-fill IA + provenance
- [x] `getPrefillCount()` parité

---

## Critères d'acceptation

- [ ] CONTEXTUAL pour `harcelement_detecte=true`.
- [ ] Checklist dynamique selon étape procédurale.
- [ ] Délai fatal 90 j affiché en rouge si ≤ 30 j restants.
- [ ] Protection représailles 12 mois visible.
- [ ] `DecisionToolVisibilityIntegrityIT` vert.

---

## Technique

- `frontend/src/app/case-files/harcelement-be-procedure-formelle-section/` — composant + prefill-rules
- `frontend/src/app/core/models/harcelement-be-procedure-formelle.model.ts`
- Migration : `XXX-add-harcelement-be-procedure-formelle-visibility.xml`

---

## Dépendances

- SF-213-07 backend mergée.
