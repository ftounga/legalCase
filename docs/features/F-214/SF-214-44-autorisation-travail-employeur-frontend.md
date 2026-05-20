# Mini-spec — F-214 / SF-214-44 — Autorisation travail employeur — frontend

## Identifiant

`F-214 / SF-214-44`

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-autorisation-travail-employeur-section>` pour `F-IM-46-autorisation-travail-employeur-fr`, outil côté employeur complémentaire à F-IM-07.

---

## Comportement attendu

- Formulaire : `typeContrat` (select), `posteProposes` (text), `nationaliteCandidat` (text, pré-rempli), `dureeContratMois` (number), `refusAutorisation` (checkbox), `dateRefusAutorisation` (date optionnel).
- Résultat : statut chip, `obligationsDemande` stepper, `delaiInstructionOFII` (JetBrains Mono), recours si refus (délai TA 2 mois).
- pré-fill : `nationalite` → nationaliteCandidat.
- ALWAYS_ON : visible sur tout dossier Immigration FR.
- Bridge F-69 : deadline recours si RECOURS_POSSIBLE.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 2 (checklist procédure employeur) + 3 (calculateur délai si refus).

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] POST AUTORISATION_NON_REQUISE (UE) → statut vert
- [x] POST refus → délai TA affiché + Bridge F-69
- [x] Tests Jest ≥ 12

## Tables / endpoints / composants impactés

- **Nouveau composant** `AutorisationTravailEmployeurSectionComponent`
- **Nouveau service** `AutorisationTravailEmployeurService`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-214-43 : statut `done`
