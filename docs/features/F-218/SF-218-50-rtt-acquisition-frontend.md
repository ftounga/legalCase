# Mini-spec — F-218 / SF-218-50 — RTT : acquisition (décompte des jours) — frontend

## Identifiant

`F-218 / SF-218-50`

## Feature parente

`F-218d` — Temps de travail / épargne salariale / congés FR-only (P4 Travail FR)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-50-rtt-acquisition-frontend`

---

## Objectif

Livrer le composant Angular standalone OnPush `<app-rtt-acquisition-section>` pour `F-DT-80-rtt-acquisition` : saisie de l'horaire hebdomadaire collectif, affichage du nombre théorique de jours de RTT acquis, ou de l'état de renvoi vers le décompte d'heures supplémentaires en l'absence d'accord de réduction du temps de travail.

---

## Comportement attendu

- Composant **standalone, `ChangeDetection.OnPush`**, sélecteur `<app-rtt-acquisition-section>` ; injection `ChangeDetectorRef` et appel `markForCheck()` dans chaque `subscribe()` (next + error) — invariant OnPush.
- Endpoint consommé : `GET /api/v1/case-files/{caseFileId}/rtt-acquisition-analysis`.
- Formulaire (champs = ceux du POST backend — SF paire) : `horaireHebdomadaireCollectif` (number, pré-rempli), `accordRttPresent` (checkbox).
- Résultat :
  - Si `accordRttPresent=false` → état `RENVOI_HEURES_SUP` : **badge orange** (état neutre) + encart de renvoi vers le décompte d'heures supplémentaires (outil heures sup), pas de calcul RTT.
  - Sinon → `nombreJrttTheorique` (badge, JetBrains Mono) calculé à partir de l'horaire collectif.
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `rtt_acquisition_detectee` = true. Thème **INDEMNITES**.
- Pré-fill : `horaireHebdomadaireCollectif` depuis `Sf218dDetail` (sous-record consolidé de `TravailExtractedData`). Tout champ saisissable est pré-rempli par l'IA (F-246) sauf information non factualisable.
- Règles de pré-remplissage isolées dans `rtt-acquisition-section-prefill-rules.ts`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (orange réservé état neutre `RENVOI_HEURES_SUP` ; pas de rouge — aucun verdict défavorable ; navy/or info ; JetBrains Mono jrtt/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData` (via `Sf218dDetail`), `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `horaireHebdomadaireCollectif`, handlers `onXChange()` ; règles dans `rtt-acquisition-section-prefill-rules.ts`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / saisie), via `CoherenceAlertBuilder` partagé + popover de divergence (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-80-rtt-acquisition` enregistré dans `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON) + `THEME_BY_TOOL_ID` (`INDEMNITES`)
- Niveau outil : 2 (calculateur jrtt + renvoi conditionnel) → parité domaines **non applicable** (acquisition RTT = spécificité FR-only sans équivalent immigration/famille)

---

## Self-check grep pré-commit (OBLIGATOIRE)

Avant tout commit, exécuter et vérifier la cohérence `TOOL_REGISTRY` ↔ test d'intégrité :

```
grep -n "F-DT-80-rtt-acquisition" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts
grep -n "F-DT-80-rtt-acquisition" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts
grep -rn "F-DT-80-rtt-acquisition" frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts
```

L'ID doit apparaître dans `TOOL_REGISTRY`, `THEME_BY_TOOL_ID` et la liste des IDs connus du test d'intégrité (KNOWN_FRONTEND_TOOL_IDS / test `THEME_BY_TOOL_ID`). Tout orphelin = BLOCAGE pré-commit.

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript (`ng build`)
- [ ] `accordRttPresent=true` + `horaireHebdomadaireCollectif=39` → `nombreJrttTheorique` calculé et affiché
- [ ] `accordRttPresent=false` → état `RENVOI_HEURES_SUP` badge orange + encart de renvoi vers le décompte d'heures supplémentaires
- [ ] `horaireHebdomadaireCollectif` pré-rempli depuis `Sf218dDetail` avec badge provenance `auto_awesome`
- [ ] aucun verdict rouge (l'outil ne produit pas de défaveur)
- [ ] divergence aiData ↔ saisie → alerte F-IA-03 (CoherenceAlertBuilder) + popover
- [ ] Tests Jest ≥ 12 (rendu form, calcul jrtt, état RENVOI_HEURES_SUP orange + renvoi, pré-fill, getPrefillCount 0/partiel/nominal, coherenceAlerts, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `RttAcquisitionSectionComponent` (+ `rtt-acquisition-section-prefill-rules.ts`)
- **Nouveau service** `RttAcquisitionService`
- **Nouveau modèle** `RttAcquisitionAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + `THEME_BY_TOOL_ID`) ; DTO `TravailExtractedData` frontend (sous-record `Sf218dDetail` : `horaireHebdomadaireCollectif`, `rttAcquisitionDetectee`)

## Dépendances

- SF-218-49 (backend RTT acquisition) : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Backend (= SF paire SF-218-49)
- Monétisation des RTT (F-DT-51 → SF-218-38)
- Décompte effectif des heures supplémentaires (outil heures sup dédié, vers lequel renvoie cet outil)
