# Mini-spec — F-163 / SF-163-03d Famille FR — reliquats mode simulateur (partie 1 : paternité + partition)

## Identifiant

`F-163 / SF-163-03d`

## Feature parente

`F-163` — Outils décisionnels en mode simulateur autonome (hors dossier)

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-163-03d-famille-fr-reliquats-partie1`

---

## Objectif

> Étendre `SimulatorCalculatorRegistry` + `STANDALONE_READY_TOOL_IDS` aux **~9 reliquats Famille FR** centrés sur **paternité (F-FA-18-*) et partition (F-FA-20/22/23)**. Catégorie Z de l'audit 2026-05-11.

---

## Périmètre

Cibles cataloguées par l'audit (catégorie Z Famille FR — partie 1) :
- **F-FA-18-* paternité** (action en recherche / contestation / reconnaissance — toolIds exacts à confirmer par grep)
- **F-FA-20-** (partition liée à divorce)
- **F-FA-22-** (partition spécifique régime)
- **F-FA-23-** (partition complémentaire)

**L'agent doit faire l'inventaire exact** : pour chaque toolId Famille FR dans `TOOL_REGISTRY` absent de `STANDALONE_READY_TOOL_IDS` ET du registry backend, classer (Z/Y/X) et inclure seulement Z. ~9 toolIds attendus. Si l'inventaire révèle moins ou plus, ajuster le périmètre et le documenter.

---

## Pattern à appliquer

Identique à SF-163-03b/c. Backend registry + frontend `[standaloneMode]` + whitelist + tests.

---

## Critères d'acceptation

- [ ] **CA-01** : tous les calculators Famille FR partie 1 catégorie Z enregistrés.
- [ ] **CA-02** : composants Angular refactorés `[standaloneMode]`.
- [ ] **CA-03** : `STANDALONE_READY_TOOL_IDS` étendue.
- [ ] **CA-04** : ≥ 1 test IT backend par calculator (≥ 9 tests).
- [ ] **CA-05** : ≥ 3 tests Jest par composant (≥ 27 tests).
- [ ] **CA-06** : tests existants verts.
- [ ] **CA-07** : build production OK + `./mvnw test` global vert.

---

## Hors scope

- Travail (SF-163-03b), Immigration / Meta (SF-163-03c).
- Famille FR partie 2 (SF-163-03e — succession F-FA-24-*).
- Catégories X et Y.

---

## Dépendances

- **SF-163-02a/b/c/d** — done.
- **SF-163-03** — done.

---

## Notes

- Conflits Git probables avec SF-163-03b/c/e (fichiers shared). Rebase propre.
- Partition entre paternité (partie 1) et succession (partie 2) faite pour respecter la règle CLAUDE.md "SF > 2 jours = REFUS".
