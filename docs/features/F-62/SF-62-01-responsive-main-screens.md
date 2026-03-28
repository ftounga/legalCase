---
id: SF-62-01
feature: F-62
title: Responsive mobile — Écrans principaux
status: In Progress
---

## Objectif

Rendre les 3 écrans principaux utilisables sur mobile (< 768 px) via CSS media queries, sans modifier la logique métier.

## Composants impactés

- `case-files-list.component.ts/.html/.scss` — colonnes de table réduites
- `case-file-detail.component.ts/.html/.scss` — title-row wrap, docs table colonnes, analysis-job-row
- `synthesis.component.html/.scss` — header-top-row wrap, titre adaptatif

## Comportement nominal

| Écran | Desktop | Mobile < 768px |
|-------|---------|----------------|
| Liste dossiers | 5 cols (titre, domaine, statut, date, action) | 3 cols (titre, statut, action) |
| Détail dossier | title-row flex horizontal | title-row wrap, actions en dessous |
| Détail dossier | Docs table 5 cols | 2 cols (nom + actions) |
| Détail dossier | analysis-job-row grid 5 cols | grid simplifié 3 cols, barre sur ligne séparée |
| Synthèse | header-top-row flex horizontal | flex-wrap, bouton export en dessous |

## Critères d'acceptation

1. Liste dossiers < 768px : colonnes "Domaine" et "Créé le" invisibles
2. Détail dossier < 768px : titre + badge + actions ne débordent pas de l'écran
3. Détail dossier < 768px : table documents lisible (nom + actions seulement)
4. Détail dossier < 768px : barre de progression des jobs IA lisible
5. Synthèse < 768px : retour et export ne se chevauchent pas

## Plan de test

CSS uniquement — tests visuels manuels sur viewport 375px (iPhone SE).
Aucun test unitaire modifié (zéro modification logique).
Vérification : `npm test` passe sans régression.

## Hors périmètre

- Composant `analysis-diff`
- Dialogs (création dossier, confirmation suppression)
- Écrans admin et membres
