# Mini-spec — F-218 / SF-218-20 — Cadre dirigeant : qualification (3 critères cumulatifs) — frontend

## Identifiant

`F-218 / SF-218-20`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-20-cadre-dirigeant-statut-frontend`

---

## Objectif

Livrer le composant Angular `<app-cadre-dirigeant-statut-section>` pour `F-DT-107-cadre-dirigeant-statut` : saisie des 3 critères cumulatifs + indice de participation à la direction, affichage de la qualification de cadre dirigeant, de l'exclusion (ou non) des règles de durée du travail et du risque de rappel d'heures supplémentaires.

---

## Comportement attendu

- Formulaire : `independanceEmploiDuTemps` (checkbox), `autonomieDecision` (checkbox), `remunerationParmiPlusElevees` (checkbox), `participationDirectionEntreprise` (checkbox, pré-rempli), `niveauRemunerationConstate` (number, pré-rempli, optionnel).
- Résultat :
  - Liste des 3 critères avec coche verte / croix rouge + `criteresRemplis` (badge x/3, JetBrains Mono).
  - Badge `qualification` : `CADRE_DIRIGEANT` vert / `CADRE_DIRIGEANT_FRAGILE` orange / `NON_CADRE_DIRIGEANT` rouge.
  - Badge `exclusionDureeTravail` (`EXCLU` / `NON_EXCLU`).
  - Badge `risqueRappelHeuresSupp` (`FAIBLE` vert / `MODERE` orange / `ELEVE` rouge) + note explicative.
  - `baseJuridique` en JetBrains Mono ; bannière gate FR.
- CONTEXTUAL : apparaît si flag IA `statut_cadre_dirigeant_detecte` = true. Groupement thématique cohérent avec les outils de qualification / durée du travail (réutiliser le thème existant des analyseurs de statut).
- Pré-fill : `participationDirectionEntreprise`, `niveauRemunerationConstate` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `NON_CADRE_DIRIGEANT` / risque ELEVE ; orange `FRAGILE`/`MODERE` ; navy/or info ; JetBrains Mono critères/baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard requis — calcul sans action validée)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit + ngOnChanges, signaux de provenance + badge `auto_awesome` sur `participationDirectionEntreprise`/`niveauRemunerationConstate`, handlers `onXChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed (cohérence aiData / procedureChecks F-96), via `CoherenceAlertBuilder` partagé + popover (pattern FR complet)
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-107-cadre-dirigeant-statut` enregistré (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)
- Niveau outil : 3 (analyseur qualification multi-critères + verdict + risque) → parité domaines **non applicable** (cadre dirigeant = spécificité FR-only sans équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] 3 critères cochés + participation → badge `CADRE_DIRIGEANT` vert, `EXCLU`, risque `FAIBLE`
- [ ] 3 critères cochés sans participation → badge `CADRE_DIRIGEANT_FRAGILE` orange, risque `MODERE`
- [ ] < 3 critères → badge `NON_CADRE_DIRIGEANT` rouge, `NON_EXCLU`, risque `ELEVE`
- [ ] `criteresRemplis` affiché en x/3
- [ ] `participationDirectionEntreprise` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] Note explicative du risque de rappel d'heures supplémentaires affichée
- [ ] Tests Jest ≥ 12 (rendu form, qualification CADRE_DIRIGEANT/FRAGILE/NON, comptage critères, mapping risque, exclusion durée, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `CadreDirigeantStatutSectionComponent`
- **Nouveau service** `CadreDirigeantStatutService`
- **Nouveau modèle** `CadreDirigeantStatutAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON + thème) ; DTO `TravailExtractedData` frontend (`cadreParticipationDirection` + `statutCadreDirigeantDetecte`)

## Dépendances

- SF-218-19 : statut `done` (merge backend avant merge frontend — séquencement intégrité)

## Hors périmètre

- Chiffrage du rappel d'heures supplémentaires en cas de requalification
- Génération d'un argumentaire de contestation du statut (générateur futur)
