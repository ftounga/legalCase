# Mini-spec — F-214 / SF-214-34 — Appel CAA + cassation CE — frontend

## Identifiant

`F-214 / SF-214-34`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-appel-caa-cassation-section>` pour `F-IM-41-appel-caa-cassation-ce-fr`, avec calculateur délais 15j/1mois et filtre pourvoi CE.

---

## Comportement attendu

- Formulaire : `dateJugementTA` (date), `typeDecisionTA` (select), `typeContentieux` (select), `delaiSpecialOQTF` (checkbox).
- Résultat : statut chip, `dateEcheanceAppelCaa` (JetBrains Mono rouge si URGENT), `courAppelCompetente`, `filtrePorvoisCassation` (bannière info si true), motifs liste.
- CONTEXTUAL : `recoursEnvisageDetecte`.
- Bridge F-69 : deadline `dateEcheanceAppelCaa` si APPEL_POSSIBLE ou URGENT.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 3 (calculateur délais).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] delaiSpecialOQTF → délai 15 j affiché
- [x] Bridge F-69 deadline
- [x] Tests Jest ≥ 12

## Dépendances

- SF-214-33 : statut `done`
