# Mini-spec — F-163 / SF-163-03c Immigration / Meta — reliquats mode simulateur

## Identifiant

`F-163 / SF-163-03c`

## Feature parente

`F-163` — Outils décisionnels en mode simulateur autonome (hors dossier)

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-163-03c-immigration-meta-reliquats`

---

## Objectif

> Étendre `SimulatorCalculatorRegistry` + `STANDALONE_READY_TOOL_IDS` aux **~5 reliquats Immigration et Meta** identifiés catégorie Z par l'audit 2026-05-11. Inclut notamment les outils transversaux (`F-152`, `F-153` fourchettes JAF) qui sont classés "Meta" car ils ne sont strictement attachés à aucun domaine.

---

## Périmètre

Cibles identifiées par l'audit (catégorie Z Immigration / Meta) :
- **F-IM-05-arbre-decisionnel-titre** (Immigration FR — composant canonique F-IA-04, soigner)
- **F-IM-06-recours** (Immigration FR)
- **F-IM-07-droit-au-travail** (Immigration FR)
- **F-152-divorce-consentement-scoring** (Meta / Famille)
- **F-153-fourchettes-jaf** (Meta / Famille)

**L'agent doit faire l'inventaire exact** : grep TOOL_REGISTRY frontend ∖ STANDALONE_READY_TOOL_IDS ∖ SimulatorCalculatorRegistry sur les domaines Immigration et Meta. Vérifier que chaque toolId a un calculator stateless backend (catégorie Z). Si certains sont en fait des wrappers info-only (`PREFILL_COUNT_ALWAYS_ZERO=true`), les exclure et le documenter.

---

## Pattern à appliquer

Identique à SF-163-03b (et SF-163-02b/c/d). Backend : ajout au registry avec injection des dépendances éventuelles. Frontend : `[standaloneMode]` + bannière + bypass + whitelist + ≥ 3 tests Jest.

**Particularité F-IM-05** : composant canonique de référence F-IA-04 — soigner spécialement son refactor (les futurs nouveaux outils s'en inspirent).

---

## Critères d'acceptation

- [ ] **CA-01** : tous les calculators Immigration / Meta catégorie Z enregistrés dans le registry backend.
- [ ] **CA-02** : composants Angular refactorés `[standaloneMode]`.
- [ ] **CA-03** : `STANDALONE_READY_TOOL_IDS` étendue.
- [ ] **CA-04** : ≥ 1 test IT backend par calculator.
- [ ] **CA-05** : ≥ 3 tests Jest par composant.
- [ ] **CA-06** : tests existants verts.
- [ ] **CA-07** : build production OK + `./mvnw test` global vert.

---

## Hors scope

- Travail (SF-163-03b), Famille (SF-163-03d/e).
- Catégories X et Y.

---

## Dépendances

- **SF-163-02a/b/c/d** — done.
- **SF-163-03** — done.

---

## Notes

- Conflits Git probables avec SF-163-03b/d/e (fichiers shared). Rebase propre.
