# Mini-spec — F-214 / SF-214-22 — Victime traite L. 425-1 — frontend

## Identifiant

`F-214 / SF-214-22`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-victime-traite-section>` pour `F-IM-35-victime-traite-l4251-fr`, avec alerte sécurité si victime en danger.

---

## Comportement attendu

- Formulaire : `plainteDeposee` (checkbox), `datePlainte` (date), `collaborationOCRTEH` (checkbox), `presenceAutoriteRefugieDetectee` (checkbox).
- Résultat : verdict chip, mesuresProtection liste, `risqueVictimeEnDanger` bannière rouge si true.
- CONTEXTUAL : `victimeTraiteDetectee`.
- Pas de bridge F-69 requis (pas de délai daté calculé V1).

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 5 (scoring) — parité domaines : traite FR-only (régime distinct en BE).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] risqueVictimeEnDanger → bannière rouge
- [x] Tests Jest ≥ 12

## Tables / endpoints / composants impactés

- **Nouveau composant** `VictimeTraiteSectionComponent`
- **Nouveau service** `VictimeTraiteService`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-214-21 : statut `done`
