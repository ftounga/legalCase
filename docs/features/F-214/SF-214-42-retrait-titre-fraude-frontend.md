# Mini-spec — F-214 / SF-214-42 — Retrait titre fraude — frontend

## Identifiant

`F-214 / SF-214-42`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-retrait-titre-fraude-section>` pour `F-IM-45-retrait-titre-fraude-fr`.

---

## Comportement attendu

- Formulaire : `dateRetrait` (date), `motifRetrait` (select), `miseEnDemeurePrEalable` (checkbox), `dateMiseEnDemeure` (date optionnel).
- Résultat : statut chip, vicesDeProcedure liste (orange si non vide), motifsContestation liste, `delaiRecoursTA` (JetBrains Mono).
- CONTEXTUAL : `retraitTitreFraudeDetecte`.
- Bridge F-69 : deadline `delaiRecoursTA` si RECOURS_POSSIBLE.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 5 (analyseur validité).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] vicesDeProcedure → section orange si non vide
- [x] Bridge F-69 délai
- [x] Tests Jest ≥ 12

## Dépendances

- SF-214-41 : statut `done`
