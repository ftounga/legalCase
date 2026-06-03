# Mini-spec — F-218 / SF-218-46 — Congé parental d'éducation — frontend

## Identifiant

`F-218 / SF-218-46`

## Feature parente

`F-218d` — Temps de travail / épargne salariale / congés FR-only (P4 Travail FR)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-46-conge-parental-education-frontend`

---

## Objectif

Livrer le composant Angular standalone OnPush `<app-conge-parental-education-section>` pour `F-DT-78-conge-parental-education` : saisie de l'ancienneté et de la date de naissance/adoption, affichage du verdict d'éligibilité au congé parental d'éducation et de la date de fin maximale du droit.

---

## Comportement attendu

- Composant **standalone, `ChangeDetection.OnPush`**, sélecteur `<app-conge-parental-education-section>` ; injection `ChangeDetectorRef` et appel `markForCheck()` dans chaque `subscribe()` (next + error) — invariant OnPush.
- Endpoint consommé : `GET /api/v1/case-files/{caseFileId}/conge-parental-education-analysis`.
- Formulaire (champs = ceux du POST backend — SF paire) : `ancienneteMois` (number, pré-rempli), `dateNaissanceOuAdoption` (date, pré-rempli).
- Résultat :
  - Badge `eligibilite` : `ELIGIBLE` vert / `NON_ELIGIBLE` rouge (+ motif si ancienneté insuffisante).
  - `dateFinMax` (date limite du droit) en JetBrains Mono.
  - Mention de la condition d'ancienneté minimale (1 an à la naissance/adoption) et des renouvellements.
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `conge_parental_detecte` = true. Thème **DIAGNOSTIC**.
- Pré-fill : `ancienneteMois`, `dateNaissanceOuAdoption` depuis `Sf218dDetail` (sous-record consolidé de `TravailExtractedData`). Tout champ saisissable est pré-rempli par l'IA (F-246) sauf information non factualisable.
- Règles de pré-remplissage isolées dans `conge-parental-education-section-prefill-rules.ts`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_ELIGIBLE` ; vert `ELIGIBLE` ; navy/or info ; `<input type="date">` ; JetBrains Mono dateFinMax/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData` (via `Sf218dDetail`), `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `ancienneteMois`/`dateNaissanceOuAdoption`, handlers `onXChange()` ; règles dans `conge-parental-education-section-prefill-rules.ts`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / saisie), via `CoherenceAlertBuilder` partagé + popover de divergence (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-78-conge-parental-education` enregistré dans `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON) + `THEME_BY_TOOL_ID` (`DIAGNOSTIC`)
- Niveau outil : 2 (analyseur éligibilité + date limite) → parité domaines **non applicable** (congé parental d'éducation FR = spécificité FR-only sans équivalent immigration/famille)

---

## Self-check grep pré-commit (OBLIGATOIRE)

Avant tout commit, exécuter et vérifier la cohérence `TOOL_REGISTRY` ↔ test d'intégrité :

```
grep -n "F-DT-78-conge-parental-education" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts
grep -n "F-DT-78-conge-parental-education" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts
grep -rn "F-DT-78-conge-parental-education" frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts
```

L'ID doit apparaître dans `TOOL_REGISTRY`, `THEME_BY_TOOL_ID` et la liste des IDs connus du test d'intégrité (KNOWN_FRONTEND_TOOL_IDS / test `THEME_BY_TOOL_ID`). Tout orphelin = BLOCAGE pré-commit.

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript (`ng build`)
- [ ] `ancienneteMois=18` + naissance récente → `ELIGIBLE` vert + `dateFinMax` affichée
- [ ] `ancienneteMois=6` → `NON_ELIGIBLE` rouge + motif ancienneté insuffisante
- [ ] `dateFinMax` calculée et affichée pour un cas éligible
- [ ] `ancienneteMois` / `dateNaissanceOuAdoption` pré-remplis depuis `Sf218dDetail` avec badge provenance `auto_awesome`
- [ ] divergence aiData ↔ saisie → alerte F-IA-03 (CoherenceAlertBuilder) + popover
- [ ] Tests Jest ≥ 12 (rendu form, ELIGIBLE/NON_ELIGIBLE, condition ancienneté, dateFinMax, pré-fill, getPrefillCount 0/partiel/nominal, coherenceAlerts, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `CongeParentalEducationSectionComponent` (+ `conge-parental-education-section-prefill-rules.ts`)
- **Nouveau service** `CongeParentalEducationService`
- **Nouveau modèle** `CongeParentalEducationAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + `THEME_BY_TOOL_ID`) ; DTO `TravailExtractedData` frontend (sous-record `Sf218dDetail` : `ancienneteMois`, `dateNaissanceOuAdoption`, `congeParentalDetecte`)

## Dépendances

- SF-218-45 (backend congé parental d'éducation) : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Backend (= SF paire SF-218-45)
- Congé proche aidant (F-DT-79 → SF-218-48)
- Calcul de la PreParE (prestation partagée d'éducation de l'enfant)
