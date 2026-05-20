# Mini-spec — F-214 / SF-214-30 — Recours naturalisation TJ — frontend

## Identifiant

`F-214 / SF-214-30`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-naturalisation-recours-tj-section>` pour `F-IM-39-naturalisation-recours-tj-fr`, avec calculateur délai 6 mois et liste motifs recours.

---

## Comportement attendu

- Formulaire : `voieNaturalisation` (select MARIAGE/ASCENDANT/MINEUR), `dateRefusDeclaration` (date), `typeRefus` (select).
- Résultat : statut chip (rouge PRESCRIT/orange URGENT), `dateEcheanceRecoursJudicaire` (JetBrains Mono), `tribunalCompetent`, liste `motifsRecoursDisponibles`.
- CONTEXTUAL : `naturalisationEnvisageeDetectee`.
- Bridge F-69 : deadline si RECOURS_POSSIBLE ou URGENT.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 3 (calculateur délais).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] POST PRESCRIT → chip rouge + message
- [x] Bridge F-69 délai
- [x] Tests Jest ≥ 12

## Dépendances

- SF-214-29 : statut `done`
