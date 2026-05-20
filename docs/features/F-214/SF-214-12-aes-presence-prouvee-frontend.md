# Mini-spec — F-214 / SF-214-12 — AES calcul présence prouvée — frontend

## Identifiant

`F-214 / SF-214-12`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer le composant Angular `<app-aes-presence-prouvee-section>` pour `F-IM-30-aes-presence-prouvee-fr`, avec saisie dynamique des périodes de présence (liste ajout/suppression), calcul interactif et affichage des 4 éligibilités AES.

---

## Comportement attendu

- UI : tableau de saisie dynamique des périodes (`[{debut, fin, typePiece}]`) avec boutons Ajouter/Supprimer.
- Résultat : `anneesTotalesProuvees` (badge), grille `eligibiliteParVoie` 4 cases (vert/rouge), `gapsPeriodes` (liste orange), `recommandationsPieces` (liste).
- pré-fill : `aesDateEntreeFrance` → période initiale de la date d'entrée à aujourd'hui.
- CONTEXTUAL : visible si `aesCalculPresenceDeclenche = true` (SF-214-11).

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 3 (calculateur) — parité domaines : AES FR-only.

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] Interface dynamique périodes : ajout/suppression ligne
- [x] POST nominal → 4 eligibilités affichées + gaps orange
- [x] pré-fill aesDateEntreeFrance → période initiale
- [x] Tests Jest ≥ 15

## Tables / endpoints / composants impactés

- **Nouveau composant** `AesPresenceProuveeSectionComponent`
- **Nouveau service** `AesPresenceProuveeService`
- **Nouveau modèle** `AesPresenceProuveeAnalysis`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-214-11 : statut `done`
