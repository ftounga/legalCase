# Mini-spec — F-218 / SF-218-44 — Congés pour évènements familiaux — frontend

## Identifiant

`F-218 / SF-218-44`

## Feature parente

`F-218d` — Temps de travail / épargne salariale / congés FR-only (P4 Travail FR)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-44-conges-evenements-familiaux-frontend`

---

## Objectif

Livrer le composant Angular standalone OnPush `<app-conges-evenements-familiaux-section>` pour `F-DT-76-conges-evenements-familiaux` : saisie du type d'évènement familial, affichage de la durée de congé applicable et du maintien de salaire associé selon l'évènement.

---

## Comportement attendu

- Composant **standalone, `ChangeDetection.OnPush`**, sélecteur `<app-conges-evenements-familiaux-section>` ; injection `ChangeDetectorRef` et appel `markForCheck()` dans chaque `subscribe()` (next + error) — invariant OnPush.
- Endpoint consommé : `GET /api/v1/case-files/{caseFileId}/conges-evenements-familiaux-analysis`.
- Formulaire (champs = ceux du POST backend — SF paire) : `typeEvenement` (select : mariage/Pacs, naissance, décès enfant, décès conjoint, décès parent, annonce handicap enfant, etc. — pré-rempli).
- Résultat :
  - `dureeApplicableJours` (badge, JetBrains Mono).
  - Indicateur `maintienSalaire` (badge : maintenu vert / non maintenu gris) selon `typeEvenement`.
  - Mention de la nature légale (durées minimales du Code du travail, conventions collectives potentiellement plus favorables).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `evenement_familial_detecte` = true. Thème **INDEMNITES**.
- Pré-fill : `typeEvenement` depuis `Sf218dDetail` (sous-record consolidé de `TravailExtractedData`). Tout champ saisissable est pré-rempli par l'IA (F-246) sauf information non factualisable.
- Règles de pré-remplissage isolées dans `conges-evenements-familiaux-section-prefill-rules.ts`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (vert maintien salaire / gris non maintenu ; navy/or info ; JetBrains Mono durée/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData` (via `Sf218dDetail`), `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `typeEvenement`, handlers `onXChange()` ; règles dans `conges-evenements-familiaux-section-prefill-rules.ts`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / saisie), via `CoherenceAlertBuilder` partagé + popover de divergence (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-76-conges-evenements-familiaux` enregistré dans `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON) + `THEME_BY_TOOL_ID` (`INDEMNITES`)
- Niveau outil : 2 (barème durée + maintien salaire) → parité domaines **non applicable** (congés évènements familiaux FR = spécificité FR-only sans équivalent immigration/famille)

---

## Self-check grep pré-commit (OBLIGATOIRE)

Avant tout commit, exécuter et vérifier la cohérence `TOOL_REGISTRY` ↔ test d'intégrité :

```
grep -n "F-DT-76-conges-evenements-familiaux" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts
grep -n "F-DT-76-conges-evenements-familiaux" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts
grep -rn "F-DT-76-conges-evenements-familiaux" frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts
```

L'ID doit apparaître dans `TOOL_REGISTRY`, `THEME_BY_TOOL_ID` et la liste des IDs connus du test d'intégrité (KNOWN_FRONTEND_TOOL_IDS / test `THEME_BY_TOOL_ID`). Tout orphelin = BLOCAGE pré-commit.

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript (`ng build`)
- [ ] `typeEvenement=MARIAGE` → `dureeApplicableJours` affichée + `maintienSalaire` maintenu vert
- [ ] `typeEvenement=DECES_ENFANT` → durée majorée affichée
- [ ] changement de `typeEvenement` → durée et maintien salaire recalculés
- [ ] `typeEvenement` pré-rempli depuis `Sf218dDetail` avec badge provenance `auto_awesome`
- [ ] divergence aiData ↔ saisie → alerte F-IA-03 (CoherenceAlertBuilder) + popover
- [ ] Tests Jest ≥ 12 (rendu form, durée par évènement, maintien salaire, recalcul au changement, pré-fill, getPrefillCount 0/partiel/nominal, coherenceAlerts, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `CongesEvenementsFamiliauxSectionComponent` (+ `conges-evenements-familiaux-section-prefill-rules.ts`)
- **Nouveau service** `CongesEvenementsFamiliauxService`
- **Nouveau modèle** `CongesEvenementsFamiliauxAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + `THEME_BY_TOOL_ID`) ; DTO `TravailExtractedData` frontend (sous-record `Sf218dDetail` : `typeEvenement`, `evenementFamilialDetecte`)

## Dépendances

- SF-218-43 (backend congés évènements familiaux) : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Backend (= SF paire SF-218-43)
- Congé parental d'éducation (F-DT-78 → SF-218-46) et congé proche aidant (F-DT-79 → SF-218-48)
- Application d'une convention collective spécifique plus favorable
