# Mini-spec — F-214 / SF-214-06 — VPF liens personnels L. 423-23 — frontend

## Identifiant

`F-214 / SF-214-06`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-214-06-vpf-liens-personnels-l42323-frontend`

---

## Objectif

Livrer le composant Angular `<app-vpf-liens-personnels-section>` conforme au pattern F-IA-04, pour l'outil `F-IM-27-vpf-liens-personnels-l42323-fr`, avec scoring interactif et pré-fill IA.

---

## Comportement attendu

### Cas nominal

- Formulaire : `dureeResidenceFranceMois` (number), `entreeEnFranceMineur` (checkbox), `enfantsEnFrance` (checkbox), `conjointEnFrance` (checkbox), `parentsEnFrance` (checkbox), `niveauIntegration` (select FORT/MOYEN/FAIBLE), `ancienneConvictionPenale` (checkbox), `situationFamilialeAlEtranger` (textarea optionnel).
- Résultat : verdict chip + score points (barre de progression), chipsCriteresNonRemplis, recommandations (liste pièces).
- pré-fill IA : `aesDureePresenceMois` → dureeResidenceFranceMois ; `clientMineurDetecte` → entreeEnFranceMineur ; enfantsEnFrance déduit de `aesDureeScolaritePlusAncienEnfantAnnees`.
- Gate country FRANCE.
- Refresh dashboard.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 5 (scoring) — parité domaines : VPF est spécifique Immigration FR.

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] POST nominal → verdict + score + recommandations affiché
- [x] prefillFromAi() pré-remplit dureeResidenceFranceMois, entreeEnFranceMineur, enfantsEnFrance
- [x] Tests Jest ≥ 15
- [x] TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS

## Plan de test minimal

- Jest : composant spec (≥ 10), service spec (≥ 5), prefill-rules spec (≥ 3 getPrefillCount)

## Tables / endpoints / composants impactés

- **Nouveau composant** `VpfLiensPersonnelsSectionComponent`
- **Nouveau service** `VpfLiensPersonnelsService`
- **Nouveau modèle** `VpfLiensPersonnelsAnalysis`
- **Nouveau fichier** `vpf-liens-personnels-prefill-rules.ts`
- **Modification** `decisional-tools-panel.component.ts` : ajout TOOL_REGISTRY

## Dépendances

- SF-214-05 : statut `done`
