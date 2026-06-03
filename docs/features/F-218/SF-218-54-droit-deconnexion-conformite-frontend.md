# Mini-spec — F-218 / SF-218-54 — Droit à la déconnexion : conformité — frontend

## Identifiant

`F-218 / SF-218-54`

## Feature parente

`F-218d` — Temps de travail / épargne salariale / congés FR-only (P4 Travail FR)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-54-droit-deconnexion-conformite-frontend`

---

## Objectif

Livrer le composant Angular standalone OnPush `<app-droit-deconnexion-conformite-section>` pour `F-DT-83-droit-deconnexion-conformite` : saisie de l'effectif et de la présence d'un accord ou d'une charte sur le droit à la déconnexion, affichage de la checklist de conformité et du verdict (obligation déclenchée selon l'effectif).

---

## Comportement attendu

- Composant **standalone, `ChangeDetection.OnPush`**, sélecteur `<app-droit-deconnexion-conformite-section>` ; injection `ChangeDetectorRef` et appel `markForCheck()` dans chaque `subscribe()` (next + error) — invariant OnPush.
- Endpoint consommé : `GET /api/v1/case-files/{caseFileId}/droit-deconnexion-conformite-analysis`.
- Formulaire (champs = ceux du POST backend — SF paire) : `effectif` (number, pré-rempli), `accordOuChartePresent` (checkbox, pré-rempli).
- Résultat :
  - Si effectif sous le seuil d'obligation → badge `NON_REQUIS` gris + encart d'information (charte facultative).
  - Sinon, checklist de conformité (coche verte / croix rouge) : obligation déclenchée (effectif), présence accord/charte, modalités de régulation de l'usage des outils numériques.
  - Badge `statut` : `CONFORME` vert / `NON_CONFORME` rouge / `NON_REQUIS` gris.
  - Compteur d'items manquants (JetBrains Mono).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `droit_deconnexion_detecte` = true. Thème **DIAGNOSTIC**.
- Pré-fill : `effectif`, `accordOuChartePresent` depuis `Sf218dDetail` (sous-record consolidé de `TravailExtractedData`). Tout champ saisissable est pré-rempli par l'IA (F-246) sauf information non factualisable.
- Règles de pré-remplissage isolées dans `droit-deconnexion-conformite-section-prefill-rules.ts`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_CONFORME` / item manquant ; vert `CONFORME` ; gris `NON_REQUIS` ; navy/or info ; JetBrains Mono compteur/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData` (via `Sf218dDetail`), `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `effectif`/`accordOuChartePresent`, handlers `onXChange()` ; règles dans `droit-deconnexion-conformite-section-prefill-rules.ts`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / saisie), via `CoherenceAlertBuilder` partagé + popover de divergence (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-83-droit-deconnexion-conformite` enregistré dans `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON) + `THEME_BY_TOOL_ID` (`DIAGNOSTIC`)
- Niveau outil : 2 (analyseur conformité multi-checklists + verdict) → parité domaines **non applicable** (droit à la déconnexion FR = spécificité FR-only sans équivalent immigration/famille)

---

## Self-check grep pré-commit (OBLIGATOIRE)

Avant tout commit, exécuter et vérifier la cohérence `TOOL_REGISTRY` ↔ test d'intégrité :

```
grep -n "F-DT-83-droit-deconnexion-conformite" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts
grep -n "F-DT-83-droit-deconnexion-conformite" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts
grep -rn "F-DT-83-droit-deconnexion-conformite" frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts
```

L'ID doit apparaître dans `TOOL_REGISTRY`, `THEME_BY_TOOL_ID` et la liste des IDs connus du test d'intégrité (KNOWN_FRONTEND_TOOL_IDS / test `THEME_BY_TOOL_ID`). Tout orphelin = BLOCAGE pré-commit.

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript (`ng build`)
- [ ] effectif ≥ seuil + `accordOuChartePresent=true` → `CONFORME` vert, item accord/charte coche verte
- [ ] effectif ≥ seuil + `accordOuChartePresent=false` → `NON_CONFORME` rouge, item croix rouge, compteur ≥ 1
- [ ] effectif < seuil → badge `NON_REQUIS` gris + encart d'information
- [ ] `effectif` / `accordOuChartePresent` pré-remplis depuis `Sf218dDetail` avec badge provenance `auto_awesome`
- [ ] divergence aiData ↔ saisie → alerte F-IA-03 (CoherenceAlertBuilder) + popover
- [ ] Tests Jest ≥ 12 (rendu form, CONFORME/NON_CONFORME/NON_REQUIS, checklist, seuil effectif, compteur, pré-fill, getPrefillCount 0/partiel/nominal, coherenceAlerts, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `DroitDeconnexionConformiteSectionComponent` (+ `droit-deconnexion-conformite-section-prefill-rules.ts`)
- **Nouveau service** `DroitDeconnexionConformiteService`
- **Nouveau modèle** `DroitDeconnexionConformiteAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + `THEME_BY_TOOL_ID`) ; DTO `TravailExtractedData` frontend (sous-record `Sf218dDetail` : `effectif`, `accordOuChartePresent`, `droitDeconnexionDetecte`)

## Dépendances

- SF-218-53 (backend droit à la déconnexion) : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Backend (= SF paire SF-218-53)
- Conformité de l'épargne salariale (F-DT-53 → SF-218-42)
- Évaluation d'un contentieux de surcharge / forfait-jours
