# Mini-spec — F-214 / SF-214-40 — UE/EEE/Suisse droit au séjour — frontend

## Identifiant

`F-214 / SF-214-40`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-ue-eee-suisse-sejour-section>` pour `F-IM-44-ue-eee-suisse-sejour-fr`, avec identification automatique citoyens UE.

---

## Comportement attendu

- Formulaire : `nationalite` (text, pré-rempli), `estCitoyenUE` (checkbox auto), `membreFamilleNonUE` (checkbox), `dureeSejourMois` (number), `activiteProfessionnelle` (select).
- Résultat : `droitSejourPlus5Ans` badge, `titreObtenu` chip, conditionsRespectees liste, `situationMenbreNonUE` encadré si applicable.
- pré-fill : `nationalite`, `nationaliteUe` → estCitoyenUE, `aesDureePresenceMois` → dureeSejourMois.
- CONTEXTUAL : `nationaliteUe = true`.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 5 (analyseur droits).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] pré-fill nationalite + nationaliteUe
- [x] membreFamilleNonUE → section dédiée affichée
- [x] Tests Jest ≥ 12

## Dépendances

- SF-214-39 : statut `done`
