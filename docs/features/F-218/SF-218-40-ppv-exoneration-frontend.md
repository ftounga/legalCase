# Mini-spec — F-218 / SF-218-40 — Prime de partage de la valeur (PPV) : exonération — frontend

## Identifiant

`F-218 / SF-218-40`

## Feature parente

`F-218d` — Temps de travail / épargne salariale / congés FR-only (P4 Travail FR)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-40-ppv-exoneration-frontend`

---

## Objectif

Livrer le composant Angular standalone OnPush `<app-ppv-exoneration-section>` pour `F-DT-52-ppv-exoneration` : saisie du montant de la PPV versée et de la présence d'un accord d'intéressement, affichage du verdict de conformité au plafond (3 000 € / 6 000 €), de la part exonérée et de la part imposable.

---

## Comportement attendu

- Composant **standalone, `ChangeDetection.OnPush`**, sélecteur `<app-ppv-exoneration-section>` ; injection `ChangeDetectorRef` et appel `markForCheck()` dans chaque `subscribe()` (next + error) — invariant OnPush.
- Endpoint consommé : `GET /api/v1/case-files/{caseFileId}/ppv-exoneration-analysis`.
- Formulaire (champs = ceux du POST backend — SF paire) : `montantPrime` (number, pré-rempli), `accordInteressementPresent` (checkbox, pré-rempli).
- Résultat :
  - Badge `statut` : `CONFORME` vert / `PLAFOND_DEPASSE` rouge.
  - `montantExonere` et `montantImposable` en JetBrains Mono.
  - `plafondApplicable` affiché (3 000 € sans accord d'intéressement / 6 000 € avec accord — badge, JetBrains Mono).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `ppv_detectee` = true. Thème **INDEMNITES**.
- Pré-fill : `montantPrime`, `accordInteressementPresent` depuis `Sf218dDetail` (sous-record consolidé de `TravailExtractedData`). Tout champ saisissable est pré-rempli par l'IA (F-246) sauf information non factualisable.
- Règles de pré-remplissage isolées dans `ppv-exoneration-section-prefill-rules.ts`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `PLAFOND_DEPASSE` ; navy/or info ; JetBrains Mono montants/plafond/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData` (via `Sf218dDetail`), `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `montantPrime`/`accordInteressementPresent`, handlers `onXChange()` ; règles dans `ppv-exoneration-section-prefill-rules.ts`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / saisie), via `CoherenceAlertBuilder` partagé + popover de divergence (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-52-ppv-exoneration` enregistré dans `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON) + `THEME_BY_TOOL_ID` (`INDEMNITES`)
- Niveau outil : 3 (calculateur exonération + verdict plafond) → parité domaines **non applicable** (PPV = spécificité FR-only sans équivalent immigration/famille)

---

## Self-check grep pré-commit (OBLIGATOIRE)

Avant tout commit, exécuter et vérifier la cohérence `TOOL_REGISTRY` ↔ test d'intégrité :

```
grep -n "F-DT-52-ppv-exoneration" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts
grep -n "F-DT-52-ppv-exoneration" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts
grep -rn "F-DT-52-ppv-exoneration" frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts
```

L'ID doit apparaître dans `TOOL_REGISTRY`, `THEME_BY_TOOL_ID` et la liste des IDs connus du test d'intégrité (KNOWN_FRONTEND_TOOL_IDS / test `THEME_BY_TOOL_ID`). Tout orphelin = BLOCAGE pré-commit.

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript (`ng build`)
- [ ] `montantPrime=2500` sans accord → `CONFORME` vert, `montantExonere=2500`, `montantImposable=0`, plafond 3 000 €
- [ ] `montantPrime=4000` sans accord → `PLAFOND_DEPASSE` rouge, `montantExonere=3000`, `montantImposable=1000`
- [ ] `accordInteressementPresent=true` → plafond applicable 6 000 €
- [ ] `montantPrime` / `accordInteressementPresent` pré-remplis depuis `Sf218dDetail` avec badge provenance `auto_awesome`
- [ ] divergence aiData ↔ saisie → alerte F-IA-03 (CoherenceAlertBuilder) + popover
- [ ] Tests Jest ≥ 12 (rendu form, CONFORME/PLAFOND_DEPASSE, plafond 3000/6000, exonéré/imposable, pré-fill, getPrefillCount 0/partiel/nominal, coherenceAlerts, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `PpvExonerationSectionComponent` (+ `ppv-exoneration-section-prefill-rules.ts`)
- **Nouveau service** `PpvExonerationService`
- **Nouveau modèle** `PpvExonerationAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + `THEME_BY_TOOL_ID`) ; DTO `TravailExtractedData` frontend (sous-record `Sf218dDetail` : `montantPrime`, `accordInteressementPresent`, `ppvDetectee`)

## Dépendances

- SF-218-39 (backend PPV exonération) : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Backend (= SF paire SF-218-39)
- Conformité globale de l'épargne salariale (F-DT-53 → SF-218-42)
- Régime social/CSG-CRDS détaillé de la PPV
