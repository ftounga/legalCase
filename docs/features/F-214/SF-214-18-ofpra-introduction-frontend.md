# Mini-spec — F-214 / SF-214-18 — OFPRA introduction — frontend

## Identifiant

`F-214 / SF-214-18`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-ofpra-introduction-section>` pour `F-IM-33-ofpra-introduction-fr`, avec liste d'étapes ordonnées et alerte procédure accélérée.

---

## Comportement attendu

- Formulaire : `dateArriveeEnFrance` (date), `passageGudaEffectue` (checkbox), `datePassageGuda` (date), `adaRequise` (checkbox).
- Résultat : stepper 5 étapes, pièces requises (liste), `procedureAccelerereRisque` (bannière orange), délai JetBrains Mono.
- pré-fill : `aesDateEntreeFrance` → dateArriveeEnFrance.
- CONTEXTUAL : `procedureAsileDetectee`.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 2 (checklist + procédure).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] Stepper 5 étapes affiché
- [x] Alerte procédure accélérée bannière orange si risque
- [x] Tests Jest ≥ 12

## Tables / endpoints / composants impactés

- **Nouveau composant** `OfpraIntroductionSectionComponent`
- **Nouveau service** `OfpraIntroductionService`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-214-17 : statut `done`
