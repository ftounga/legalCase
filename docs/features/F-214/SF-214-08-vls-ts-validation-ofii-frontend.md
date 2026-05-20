# Mini-spec — F-214 / SF-214-08 — Validation VLS-TS OFII — frontend

## Identifiant

`F-214 / SF-214-08`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer le composant Angular `<app-vls-ts-validation-section>` pour l'outil `F-IM-28-vls-ts-validation-ofii-fr`, avec calculateur de délai (palette rouge URGENT/EXPIRE) et pré-fill IA de la date d'entrée.

---

## Comportement attendu

- Formulaire : `dateEntreeFrance` (date), `typeVlsTs` (select), `validationOFIIEffectuee` (checkbox), `dateValidationOFII` (date, conditionnel).
- Résultat : statut (chip couleur rouge EXPIRE/orange URGENT/vert VALIDE), `dateEcheanceValidation` (JetBrains Mono), `joursRestantsValidation` (badge), `procedureRecours` si EXPIRE.
- **Palette urgence** : rouge dominant pour EXPIRE et URGENT (similaire F-208 JLD rétention).
- pré-fill : `aesDateEntreeFrance` → dateEntreeFrance.
- ALWAYS_ON : pas de gate CONTEXTUAL flag (visible dès que workspaceCountry=FRANCE et domaine Immigration).
- Bridge échéance → `app-case-deadlines-section` : si URGENT ou A_VALIDER → deadline `dateEcheanceValidation` créée dans F-69.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 3 (calculateur délais) — parité domaines : VLS-TS est spécifique FR.

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] POST EXPIRE → verdict rouge + procedureRecours affiché
- [x] POST URGENT → verdict orange + compteur jours
- [x] dateEntreeFrance pré-rempli depuis aesDateEntreeFrance
- [x] Bridge F-69 : deadline créée pour statuts A_VALIDER et URGENT
- [x] Tests Jest ≥ 15
- [x] TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS

## Plan de test minimal

- Jest : composant spec (≥ 10), service spec (≥ 5), prefill-rules spec (≥ 3 getPrefillCount)

## Tables / endpoints / composants impactés

- **Nouveau composant** `VlsTsValidationSectionComponent`
- **Nouveau service** `VlsTsValidationService`
- **Nouveau modèle** `VlsTsValidationAnalysis`
- **Nouveau fichier** `vls-ts-validation-prefill-rules.ts`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-214-07 : statut `done`
