# Mini-spec — F-214 / SF-214-16 — Récépissé vs attestation — frontend

## Identifiant

`F-214 / SF-214-16`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Livrer `<app-recepisse-attestation-section>` pour `F-IM-32-recepisse-attestation-fr`, affichant les droits attachés au document et alertant sur le risque employeur.

---

## Comportement attendu

- Formulaire : `typeDocument` (select RECEPISSE/ATTESTATION/INCONNU), `dateDelivrance` (date), `dateExpiration` (date).
- Résultat : badges droitSejour (✓), droitTravail (✓/✗), dureeValiditeJours, `risqueEmployeur` bannière orange si ATTESTATION.
- pré-fill : `dateExpirationTitre`.
- CONTEXTUAL : `recouvrementTitreEnCours = true`.

---

## Conformité F-IA-04

- [x] Toutes les 6 obligations canoniques
- Niveau outil : 5 (analyseur droits) — parité domaines : récépissé FR-only.

---

## Critères d'acceptation

- [x] BUILD SUCCESS 0 erreur TypeScript
- [x] POST ATTESTATION → bannière orange risqueEmployeur
- [x] Tests Jest ≥ 12

## Tables / endpoints / composants impactés

- **Nouveau composant** `RecepisseAttestationSectionComponent`
- **Nouveau service** `RecepisseAttestationService`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-214-15 : statut `done`
