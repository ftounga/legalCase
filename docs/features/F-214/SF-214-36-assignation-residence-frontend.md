# Mini-spec — F-214 / SF-214-36 — Assignation résidence — frontend

## Identifiant

`F-214 / SF-214-36`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-assignation-residence-section>` pour `F-IM-42-assignation-residence-fr`.

---

## Comportement attendu

- Formulaire : `dateNotificationAssignation` (date), `dureeAssignationJours` (number), `motifAssignation` (select), `obligationsPresentation` (textarea).
- Résultat : statut chip, `dateEcheanceAssignation` (JetBrains Mono), `renouvellementPossible` badge, motifs contestation liste.
- CONTEXTUAL : `assignationResidenceDetectee`.
- Bridge F-69 : deadline `dateEcheanceAssignation` si EN_COURS.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 5 (analyseur validité).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] POST EXPIRATION_PROCHE → chip orange
- [x] Bridge F-69 deadline
- [x] Tests Jest ≥ 12

## Dépendances

- SF-214-35 : statut `done`
