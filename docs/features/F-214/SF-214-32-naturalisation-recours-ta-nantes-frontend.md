# Mini-spec — F-214 / SF-214-32 — Recours naturalisation TA Nantes — frontend

## Identifiant

`F-214 / SF-214-32`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-naturalisation-recours-ta-section>` pour `F-IM-40-naturalisation-recours-ta-fr`.

---

## Comportement attendu

- Formulaire : `dateRefusDecret` (date), `motivationRefus` (textarea optionnel), `recoursPrerequis` (checkbox).
- Résultat : statut chip, `dateEcheanceRecoursTa` (JetBrains Mono), `tribunalCompetent` (TA Nantes en gras), motifs recours liste.
- CONTEXTUAL : `naturalisationEnvisageeDetectee`.
- Bridge F-69 : deadline si RECOURS_POSSIBLE.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 3 (calculateur délais).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] tribunalCompetent = TA Nantes affiché explicitement
- [x] Bridge F-69 délai
- [x] Tests Jest ≥ 12

## Dépendances

- SF-214-31 : statut `done`
- SF-214-29 : statut `done` (champs ImmigrationExtractedData partagés)
