# Mini-spec — F-218 / SF-218-48 — Congé de proche aidant — frontend

## Identifiant

`F-218 / SF-218-48`

## Feature parente

`F-218d` — Temps de travail / épargne salariale / congés FR-only (P4 Travail FR)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-48-conge-proche-aidant-frontend`

---

## Objectif

Livrer le composant Angular standalone OnPush `<app-conge-proche-aidant-section>` pour `F-DT-79-conge-proche-aidant` : saisie du lien avec la personne aidée, affichage du verdict d'éligibilité, de la durée maximale du congé et de l'estimation de l'allocation journalière du proche aidant (AJPA).

---

## Comportement attendu

- Composant **standalone, `ChangeDetection.OnPush`**, sélecteur `<app-conge-proche-aidant-section>` ; injection `ChangeDetectorRef` et appel `markForCheck()` dans chaque `subscribe()` (next + error) — invariant OnPush.
- Endpoint consommé : `GET /api/v1/case-files/{caseFileId}/conge-proche-aidant-analysis`.
- Formulaire (champs = ceux du POST backend — SF paire) : `lienPersonneAidee` (select : conjoint, ascendant, descendant, etc. — pré-rempli).
- Résultat :
  - Badge `eligibilite` : `ELIGIBLE` vert / `NON_ELIGIBLE` rouge (+ motif si lien hors liste).
  - `dureeMaxMois` (badge, JetBrains Mono).
  - `estimationAjpa` (allocation journalière du proche aidant) en JetBrains Mono, avec mention « estimation indicative ».
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `proche_aidant_detecte` = true. Thème **INDEMNITES**.
- Pré-fill : `lienPersonneAidee` depuis `Sf218dDetail` (sous-record consolidé de `TravailExtractedData`). Tout champ saisissable est pré-rempli par l'IA (F-246) sauf information non factualisable.
- Règles de pré-remplissage isolées dans `conge-proche-aidant-section-prefill-rules.ts`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_ELIGIBLE` ; vert `ELIGIBLE` ; navy/or info ; JetBrains Mono durée/AJPA/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData` (via `Sf218dDetail`), `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `lienPersonneAidee`, handlers `onXChange()` ; règles dans `conge-proche-aidant-section-prefill-rules.ts`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / saisie), via `CoherenceAlertBuilder` partagé + popover de divergence (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-79-conge-proche-aidant` enregistré dans `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON) + `THEME_BY_TOOL_ID` (`INDEMNITES`)
- Niveau outil : 3 (analyseur éligibilité + durée + estimation AJPA) → parité domaines **non applicable** (congé proche aidant FR = spécificité FR-only sans équivalent immigration/famille)

---

## Self-check grep pré-commit (OBLIGATOIRE)

Avant tout commit, exécuter et vérifier la cohérence `TOOL_REGISTRY` ↔ test d'intégrité :

```
grep -n "F-DT-79-conge-proche-aidant" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts
grep -n "F-DT-79-conge-proche-aidant" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts
grep -rn "F-DT-79-conge-proche-aidant" frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts
```

L'ID doit apparaître dans `TOOL_REGISTRY`, `THEME_BY_TOOL_ID` et la liste des IDs connus du test d'intégrité (KNOWN_FRONTEND_TOOL_IDS / test `THEME_BY_TOOL_ID`). Tout orphelin = BLOCAGE pré-commit.

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript (`ng build`)
- [ ] `lienPersonneAidee=CONJOINT` → `ELIGIBLE` vert + `dureeMaxMois` + `estimationAjpa` affichés
- [ ] `lienPersonneAidee` hors liste éligible → `NON_ELIGIBLE` rouge + motif
- [ ] `estimationAjpa` affichée avec mention « estimation indicative »
- [ ] `lienPersonneAidee` pré-rempli depuis `Sf218dDetail` avec badge provenance `auto_awesome`
- [ ] divergence aiData ↔ saisie → alerte F-IA-03 (CoherenceAlertBuilder) + popover
- [ ] Tests Jest ≥ 12 (rendu form, ELIGIBLE/NON_ELIGIBLE, durée max, estimation AJPA, pré-fill, getPrefillCount 0/partiel/nominal, coherenceAlerts, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `CongeProcheAidantSectionComponent` (+ `conge-proche-aidant-section-prefill-rules.ts`)
- **Nouveau service** `CongeProcheAidantService`
- **Nouveau modèle** `CongeProcheAidantAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + `THEME_BY_TOOL_ID`) ; DTO `TravailExtractedData` frontend (sous-record `Sf218dDetail` : `lienPersonneAidee`, `procheAidantDetecte`)

## Dépendances

- SF-218-47 (backend congé proche aidant) : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Backend (= SF paire SF-218-47)
- Congé parental d'éducation (F-DT-78 → SF-218-46)
- Calcul exact de l'AJPA versée par la CAF (estimation indicative uniquement)
