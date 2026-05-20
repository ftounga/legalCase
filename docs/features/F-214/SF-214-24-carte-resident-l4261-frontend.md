# Mini-spec — F-214 / SF-214-24 — Carte de résident L. 426-1 — frontend

## Identifiant

`F-214 / SF-214-24`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-carte-resident-section>` pour `F-IM-36-carte-resident-l4261-fr`, avec checklist critères et liste d'atouts.

---

## Comportement attendu

- Formulaire : `dureeSejourRegulierAnnees` (number), `niveauIntegration` (select), `ressourcesMensuellesNettes` (number), `condamnationsPenalesGraves` (checkbox).
- Résultat : verdict chip, `chipsCriteresNonRemplis`, liste `atouts`, baseJuridique (JetBrains Mono).
- pré-fill : `aesDureePresenceMois` → dureeSejourRegulierAnnees.
- CONTEXTUAL : `carteResidentEnvisagee`.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 5 (scoring) — parité : carte résident FR-only.

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] POST INADMISSIBLE → chip rouge
- [x] Tests Jest ≥ 12

## Tables / endpoints / composants impactés

- **Nouveau composant** `CarteResidentSectionComponent`
- **Nouveau service** `CarteResidentService`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-214-23 : statut `done`
