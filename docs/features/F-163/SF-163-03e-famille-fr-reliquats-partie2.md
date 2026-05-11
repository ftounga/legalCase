# Mini-spec — F-163 / SF-163-03e Famille FR — reliquats mode simulateur (partie 2 : succession + autres)

## Identifiant

`F-163 / SF-163-03e`

## Feature parente

`F-163` — Outils décisionnels en mode simulateur autonome (hors dossier)

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-163-03e-famille-fr-reliquats-partie2`

---

## Objectif

> Étendre `SimulatorCalculatorRegistry` + `STANDALONE_READY_TOOL_IDS` aux **~8 reliquats Famille FR** centrés sur **succession (F-FA-24-*) et autres outils résiduels** (F-FA-25/27, etc.). Catégorie Z de l'audit 2026-05-11.

---

## Périmètre

Cibles cataloguées par l'audit (catégorie Z Famille FR — partie 2) :
- **F-FA-24-* succession** (toolIds exacts à confirmer par grep)
- **F-FA-25-** (résiduel famille)
- **F-FA-27-** (résiduel famille)
- Autres reliquats Famille FR non couverts par SF-163-03d.

**L'agent doit faire l'inventaire exact** : pour chaque toolId Famille FR dans `TOOL_REGISTRY` absent de `STANDALONE_READY_TOOL_IDS` ET du registry backend ET du périmètre SF-163-03d (paternité + partition F-FA-18/20/22/23), classer (Z/Y/X) et inclure seulement Z. ~8 toolIds attendus.

---

## Pattern à appliquer

Identique à SF-163-03b/c/d.

---

## Critères d'acceptation

- [ ] **CA-01** : tous les calculators Famille FR partie 2 catégorie Z enregistrés.
- [ ] **CA-02** : composants Angular refactorés `[standaloneMode]`.
- [ ] **CA-03** : `STANDALONE_READY_TOOL_IDS` étendue.
- [ ] **CA-04** : ≥ 1 test IT backend par calculator (≥ 8 tests).
- [ ] **CA-05** : ≥ 3 tests Jest par composant (≥ 24 tests).
- [ ] **CA-06** : tests existants verts.
- [ ] **CA-07** : build production OK + `./mvnw test` global vert.

---

## Hors scope

- Travail (SF-163-03b), Immigration / Meta (SF-163-03c), Famille partie 1 (SF-163-03d).
- Catégories X et Y.

---

## Dépendances

- **SF-163-02a/b/c/d** — done.
- **SF-163-03** — done.

---

## Notes

- Conflits Git probables avec SF-163-03b/c/d. Rebase propre.
