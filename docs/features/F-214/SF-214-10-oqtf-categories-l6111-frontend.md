# Mini-spec — F-214 / SF-214-10 — OQTF catégories L. 611-1 — frontend

## Identifiant

`F-214 / SF-214-10`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer le composant Angular `<app-oqtf-categories-section>` pour `F-IM-29-oqtf-categories-l6111-fr`, affichant les moyens de défense spécifiques à la catégorie L. 611-1 et le renvoi vers F-IM-22 si CAT_7.

---

## Comportement attendu

- Formulaire : `categorieL611` (select 1° à 7°), `dateNotificationOqtf` (date, pré-rempli), `motifOqtf` (textarea optionnel).
- Résultat : `categorieLibelle` (titre), liste `moyensDefense` (chips ou liste ordonnée), `delaiRecours` (JetBrains Mono), `procedureParallele` chip lien vers F-IM-22 si CAT_7.
- CONTEXTUAL : apparaît si `typeProcedureDetectee` = OQTF_AVEC_DELAI ou OQTF_SANS_DELAI (même trigger que F-IM-08). Groupement thématique `CONTENTIEUX`.
- pré-fill : `dateNotificationOqtf`, `motifOqtfCode`.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 5 (analyseur de validité) — parité domaines : OQTF FR-only.

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] POST CAT_7 → procedureParallele DUBLIN affiché avec lien
- [x] dateNotificationOqtf pré-rempli depuis `ImmigrationExtractedData`
- [x] Tests Jest ≥ 15

## Tables / endpoints / composants impactés

- **Nouveau composant** `OqtfCategoriesSectionComponent`
- **Nouveau service** `OqtfCategoriesService`
- **Nouveau modèle** `OqtfCategoriesAnalysis`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-214-09 : statut `done`
