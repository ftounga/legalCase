# Mini-spec — F-214 / SF-214-28 — MNA évaluation âge — frontend

## Identifiant

`F-214 / SF-214-28`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-mna-evaluation-age-section>` pour `F-IM-38-mna-evaluation-age-fr`, avec stepper procédure ASE/JE et liste contestation examens osseux.

---

## Comportement attendu

- Formulaire : `dateNaissanceDeclaree` (date), `evaluationASERefusee` (checkbox), `dateRefusASE` (date), `examenOsseuxOrdonne` (checkbox), `resultatExamenOsseux` (textarea).
- Résultat : statut chip, stepper procédure ASE, `contestationExamenOsseux` liste si applicable, `dateEcheanceSaisineJE` (rouge si RECOURS_JE_URGENT), droitsAttaches liste.
- pré-fill : `mineursDateNaissance` → dateNaissanceDeclaree.
- CONTEXTUAL : `clientMineurDetecte`.
- Bridge F-69 : deadline `dateEcheanceSaisineJE` si RECOURS_JE_URGENT.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 5 (analyseur de validité + procédure).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] pré-fill dateNaissanceDeclaree depuis mineursDateNaissance
- [x] Bridge F-69 : deadline créée si RECOURS_JE_URGENT
- [x] Tests Jest ≥ 12

## Dépendances

- SF-214-27 : statut `done`
