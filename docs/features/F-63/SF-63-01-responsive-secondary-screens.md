---
id: SF-63-01
feature: F-63
title: Responsive mobile — Écrans secondaires
status: In Progress
---

## Objectif

Rendre les écrans secondaires utilisables sur mobile (< 768 px) via CSS media queries uniquement.

## Problèmes identifiés

| Écran | Problème mobile |
|-------|----------------|
| analysis-diff | `.selectors-row` : 2 cartes côte à côte + flèche → déborde sur mobile |
| workspace-members | Table membres 5 cols, table invitations 4 cols → trop large |
| workspace-admin | Table membres : email + rôle, email tronqué → masquer email |

Login, onboarding, billing : déjà responsive — aucun changement.

## Corrections

- `analysis-diff.component.scss` : `.selectors-row` empilé verticalement, `.selector-arrow` masqué
- `workspace-members.component.scss` : masquer colonnes `joinedAt` et `expiresAt`
- `workspace-admin.component.scss` : masquer colonne `email` dans la table membres

## Critères d'acceptation

1. Diff < 768px : sélecteurs de version empilés verticalement, pas de scroll horizontal
2. Membres < 768px : table membres lisible (nom + rôle + actions)
3. Admin < 768px : table membres lisible (rôle + actions)

## Plan de test

Pure CSS — vérification `npm test` sans régression (242 tests).

## Hors périmètre

- Super-admin (usage interne uniquement)
- Audit-logs screen (F-38, déjà simple)
