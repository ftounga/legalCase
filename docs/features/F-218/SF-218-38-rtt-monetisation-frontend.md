# Mini-spec — F-218 / SF-218-38 — RTT : monétisation (rachat de jours) — frontend

## Identifiant

`F-218 / SF-218-38`

## Feature parente

`F-218d` — Temps de travail / épargne salariale / congés FR-only (P4 Travail FR)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-38-rtt-monetisation-frontend`

---

## Objectif

Livrer le composant Angular standalone OnPush `<app-rtt-monetisation-section>` pour `F-DT-51-rtt-monetisation` : saisie des jours de RTT renoncés et du salaire journalier, affichage du verdict d'éligibilité au dispositif de monétisation (fenêtre 01/01/2022–31/12/2026), du montant brut majoré et du taux de majoration applicable.

---

## Comportement attendu

- Composant **standalone, `ChangeDetection.OnPush`**, sélecteur `<app-rtt-monetisation-section>` ; injection `ChangeDetectorRef` et appel `markForCheck()` dans chaque `subscribe()` (next + error) — invariant OnPush.
- Endpoint consommé : `GET /api/v1/case-files/{caseFileId}/rtt-monetisation-analysis`.
- Formulaire (champs = ceux du POST backend — SF paire) : `nombreJoursRttRenonces` (number, pré-rempli), `salaireJournalierBrut` (number, pré-rempli), `dateRenonciation` (date).
- Résultat :
  - Badge `eligibilite` : `ELIGIBLE` vert / `NON_ELIGIBLE` rouge (+ motif si hors fenêtre 01/01/2022–31/12/2026).
  - `montantBrut` (montant majoré) en JetBrains Mono.
  - `tauxMajoration` affiché (badge, JetBrains Mono).
  - Mention de la fenêtre temporelle du dispositif (01/01/2022–31/12/2026).
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `rtt_detecte` = true. Thème **INDEMNITES** (réutilisation du thème des calculateurs d'indemnités).
- Pré-fill : `nombreJoursRttRenonces`, `salaireJournalierBrut` depuis `Sf218dDetail` (sous-record consolidé de `TravailExtractedData`). Tout champ saisissable est pré-rempli par l'IA (F-246) sauf information non factualisable.
- Règles de pré-remplissage isolées dans `rtt-monetisation-section-prefill-rules.ts`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_ELIGIBLE` ; navy/or info ; JetBrains Mono montants/taux/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData` (via `Sf218dDetail`), `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `nombreJoursRttRenonces`/`salaireJournalierBrut`, handlers `onXChange()` ; règles dans `rtt-monetisation-section-prefill-rules.ts`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / saisie), via `CoherenceAlertBuilder` partagé + popover de divergence (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-51-rtt-monetisation` enregistré dans `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON) + `THEME_BY_TOOL_ID` (`INDEMNITES`)
- Niveau outil : 3 (calculateur + verdict d'éligibilité) → parité domaines **non applicable** (monétisation RTT = spécificité FR-only sans équivalent immigration/famille)

---

## Self-check grep pré-commit (OBLIGATOIRE)

Avant tout commit, exécuter et vérifier la cohérence `TOOL_REGISTRY` ↔ test d'intégrité :

```
grep -n "F-DT-51-rtt-monetisation" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts
grep -n "F-DT-51-rtt-monetisation" frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts
grep -rn "F-DT-51-rtt-monetisation" frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts
```

L'ID doit apparaître dans `TOOL_REGISTRY`, `THEME_BY_TOOL_ID` et la liste des IDs connus du test d'intégrité (KNOWN_FRONTEND_TOOL_IDS / test `THEME_BY_TOOL_ID`). Tout orphelin = BLOCAGE pré-commit.

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript (`ng build`)
- [ ] `dateRenonciation` dans la fenêtre + jours/salaire renseignés → badge `ELIGIBLE` vert, `montantBrut` et `tauxMajoration` affichés
- [ ] `dateRenonciation` hors fenêtre (avant 01/01/2022 ou après 31/12/2026) → badge `NON_ELIGIBLE` rouge + motif
- [ ] `nombreJoursRttRenonces` / `salaireJournalierBrut` pré-remplis depuis `Sf218dDetail` avec badge provenance `auto_awesome`
- [ ] divergence aiData ↔ saisie → alerte F-IA-03 (CoherenceAlertBuilder) + popover
- [ ] Tests Jest ≥ 12 (rendu form, ELIGIBLE/NON_ELIGIBLE, fenêtre temporelle, montant majoré, taux, pré-fill, getPrefillCount 0/partiel/nominal, coherenceAlerts, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `RttMonetisationSectionComponent` (+ `rtt-monetisation-section-prefill-rules.ts`)
- **Nouveau service** `RttMonetisationService`
- **Nouveau modèle** `RttMonetisationAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + `THEME_BY_TOOL_ID`) ; DTO `TravailExtractedData` frontend (sous-record `Sf218dDetail` : `nombreJoursRttRenonces`, `salaireJournalierBrut`, `rttDetecte`)

## Dépendances

- SF-218-37 (backend RTT monétisation) : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Backend (= SF paire SF-218-37)
- RTT acquisition / décompte des jours (F-DT-80 → SF-218-50)
- Génération d'un avenant de monétisation
