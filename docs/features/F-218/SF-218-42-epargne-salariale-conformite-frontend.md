# Mini-spec — F-218 / SF-218-42 — Intéressement / participation : conformité — frontend

## Identifiant

`F-218 / SF-218-42`

## Feature parente

`F-218d` — Temps de travail / épargne salariale / congés FR-only (P4 Travail FR)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-42-epargne-salariale-conformite-frontend`

---

## Objectif

Livrer le composant Angular standalone OnPush `<app-epargne-salariale-conformite-section>` pour `F-DT-53-epargne-salariale-conformite` : saisie de l'effectif et de la présence des accords d'intéressement / participation, affichage de la checklist de conformité et du verdict d'obligation (participation obligatoire à partir du seuil d'effectif).

---

## Comportement attendu

- Composant **standalone, `ChangeDetection.OnPush`**, sélecteur `<app-epargne-salariale-conformite-section>` ; injection `ChangeDetectorRef` et appel `markForCheck()` dans chaque `subscribe()` (next + error) — invariant OnPush.
- Endpoint consommé : `GET /api/v1/case-files/{caseFileId}/epargne-salariale-conformite-analysis`.
- Formulaire (champs = ceux du POST backend — SF paire) : `effectif` (number, pré-rempli), `accordParticipationPresent` (checkbox, pré-rempli), `accordInteressementPresent` (checkbox, pré-rempli).
- Résultat :
  - Checklist de conformité (coche verte / croix rouge) : obligation de participation (selon effectif), présence accord participation, présence accord intéressement (facultatif).
  - Badge `statut` : `CONFORME` vert / `OBLIGATION_NON_REMPLIE` rouge.
  - Compteur d'items manquants (JetBrains Mono).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `epargne_salariale_detectee` = true. Thème **DIAGNOSTIC**.
- Pré-fill : `effectif`, `accordParticipationPresent`, `accordInteressementPresent` depuis `Sf218dDetail` (sous-record consolidé de `TravailExtractedData`). Tout champ saisissable est pré-rempli par l'IA (F-246) sauf information non factualisable.
- Règles de pré-remplissage isolées dans `epargne-salariale-conformite-section-prefill-rules.ts`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `OBLIGATION_NON_REMPLIE` / item manquant ; vert conforme ; navy/or info ; JetBrains Mono compteur/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData` (via `Sf218dDetail`), `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `effectif`/`accordParticipationPresent`/`accordInteressementPresent`, handlers `onXChange()` ; règles dans `epargne-salariale-conformite-section-prefill-rules.ts`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / saisie), via `CoherenceAlertBuilder` partagé + popover de divergence (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-53-epargne-salariale-conformite` enregistré dans `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON) + `THEME_BY_TOOL_ID` (`DIAGNOSTIC`)
- Niveau outil : 2 (analyseur conformité multi-checklists + verdict) → parité domaines **non applicable** (épargne salariale = spécificité FR-only sans équivalent immigration/famille)

---

## Self-check grep pré-commit (OBLIGATOIRE)

Avant tout commit, exécuter et vérifier la cohérence `TOOL_REGISTRY` ↔ test d'intégrité :

```
grep -n "F-DT-53-epargne-salariale-conformite" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts
grep -n "F-DT-53-epargne-salariale-conformite" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts
grep -rn "F-DT-53-epargne-salariale-conformite" frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts
```

L'ID doit apparaître dans `TOOL_REGISTRY`, `THEME_BY_TOOL_ID` et la liste des IDs connus du test d'intégrité (KNOWN_FRONTEND_TOOL_IDS / test `THEME_BY_TOOL_ID`). Tout orphelin = BLOCAGE pré-commit.

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript (`ng build`)
- [ ] effectif ≥ seuil + `accordParticipationPresent=true` → `CONFORME` vert, item participation coche verte
- [ ] effectif ≥ seuil + `accordParticipationPresent=false` → `OBLIGATION_NON_REMPLIE` rouge, item participation croix rouge, compteur ≥ 1
- [ ] effectif < seuil → participation facultative, pas d'obligation déclenchée
- [ ] `effectif` / accords pré-remplis depuis `Sf218dDetail` avec badge provenance `auto_awesome`
- [ ] divergence aiData ↔ saisie → alerte F-IA-03 (CoherenceAlertBuilder) + popover
- [ ] Tests Jest ≥ 12 (rendu form, CONFORME/OBLIGATION_NON_REMPLIE, checklist, seuil effectif, compteur, pré-fill, getPrefillCount 0/partiel/nominal, coherenceAlerts, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `EpargneSalarialeConformiteSectionComponent` (+ `epargne-salariale-conformite-section-prefill-rules.ts`)
- **Nouveau service** `EpargneSalarialeConformiteService`
- **Nouveau modèle** `EpargneSalarialeConformiteAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + `THEME_BY_TOOL_ID`) ; DTO `TravailExtractedData` frontend (sous-record `Sf218dDetail` : `effectif`, `accordParticipationPresent`, `accordInteressementPresent`, `epargneSalarialeDetectee`)

## Dépendances

- SF-218-41 (backend épargne salariale conformité) : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Backend (= SF paire SF-218-41)
- Exonération de la PPV (F-DT-52 → SF-218-40)
- Calcul de la réserve spéciale de participation (RSP)
