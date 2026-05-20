# Mini-spec — F-214 / SF-214-20 — AJ CNDA — frontend

## Identifiant

`F-214 / SF-214-20`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-aj-cnda-section>` pour `F-IM-34-aj-cnda-fr`, avec vérification ressources et affichage des délais critiques.

---

## Comportement attendu

- Formulaire : `dateDecisionOFPRA` (date), `ressourcesMensuellesNettes` (number), `procedureAcceleree` (checkbox), `demandeAJDeposee` (checkbox), `dateDepotAJ` (date optionnel).
- Résultat : statut chip, `dateEcheanceDemandeAJ` (JetBrains Mono, rouge si urgent), liste pièces AJ.
- pré-fill : `asileDateDecisionAnterieure` → dateDecisionOFPRA.
- CONTEXTUAL : `procedureAsileDetectee`.
- Bridge F-69 : deadline `dateEcheanceDemandeAJ` si statut AJ_A_DEMANDER.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 3 (calculateur + checklist).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] POST NON_ELIGIBLE_RESSOURCES → chip rouge
- [x] Bridge F-69 : deadline créée
- [x] Tests Jest ≥ 12

## Tables / endpoints / composants impactés

- **Nouveau composant** `AjCndaSectionComponent`
- **Nouveau service** `AjCndaService`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-214-19 : statut `done`
