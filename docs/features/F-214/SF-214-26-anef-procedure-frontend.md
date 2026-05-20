# Mini-spec — F-214 / SF-214-26 — ANEF procédure — frontend

## Identifiant

`F-214 / SF-214-26`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-anef-procedure-section>` pour `F-IM-37-anef-procedure-fr`, avec guide pas-à-pas panne et recours.

---

## Comportement attendu

- Formulaire : `panneeANEFSignalee` (checkbox), `dateTentativeDepot` (date), `demandeAdresseePrefecture` (checkbox).
- Résultat : statut chip, stepper `etapesAlternatives` (si panne), délai recours pour faute (JetBrains Mono).
- pré-fill : `dateExpirationTitre`, `typeTitreSejour`.
- CONTEXTUAL : `anefPanneDetectee`.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 2 (checklist + procédure).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] POST PANNE_EN_COURS → stepper alternatif affiché
- [x] Tests Jest ≥ 12

## Dépendances

- SF-214-25 : statut `done`
