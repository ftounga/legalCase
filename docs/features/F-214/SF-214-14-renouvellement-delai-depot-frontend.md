# Mini-spec — F-214 / SF-214-14 — Renouvellement délai dépôt — frontend

## Identifiant

`F-214 / SF-214-14`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer le composant Angular `<app-renouvellement-delai-section>` pour `F-IM-31-renouvellement-delai-depot-fr`, avec compteur de jours coloré et bridge F-69 délai.

---

## Comportement attendu

- Formulaire : `dateExpirationTitre` (date, pré-rempli), `dateDepotDossier` (date, optionnel), `typeTitre` (text, pré-rempli).
- Résultat : statut chip (rouge EXPIRE/orange URGENT/vert), `dateOptimalDepot` (JetBrains Mono), `joursRestantsAvantOptimal` (badge compteur), `risqueIrruption` (bannière rouge si EXPIRE).
- pré-fill : `dateExpirationTitre` → dateExpirationTitre ; `typeTitreSejour` → typeTitre.
- ALWAYS_ON : visible sur tout dossier Immigration FR.
- Bridge F-69 : si statut A_DEPOSER_URGENT ou A_DEPOSER → deadline `dateOptimalDepot` dans onglet Suivi.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 3 (calculateur) — parité domaines : renouvellement FR-only.

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] dateExpirationTitre pré-rempli depuis ImmigrationExtractedData
- [x] POST EXPIRE → bannière rouge risqueIrruption
- [x] Bridge F-69 : deadline créée pour A_DEPOSER_URGENT
- [x] Tests Jest ≥ 15

## Tables / endpoints / composants impactés

- **Nouveau composant** `RenouvellementDelaiSectionComponent`
- **Nouveau service** `RenouvellementDelaiService`
- **Nouveau modèle** `RenouvellementDelaiAnalysis`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-214-13 : statut `done`
