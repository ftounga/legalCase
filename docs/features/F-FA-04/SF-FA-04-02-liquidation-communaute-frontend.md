# Mini-spec — F-FA-04 / SF-FA-04-02 Liquidation de communauté — frontend tableau + export PDF

## Identifiant

`F-FA-04 / SF-FA-04-02`

## Feature parente

`F-FA-04` — Synthèse liquidation de communauté

## Statut

`in-progress`

## Date de création

2026-04-04

## Branche Git

`feat/SF-FA-04-02-liquidation-communaute-frontend`

---

## Objectif

Afficher `liquidationCommunaute` en panneau structuré dans `SynthesisComponent` (4 sous-tableaux) et inclure la section dans l'export PDF.

---

## Critères d'acceptation

- [ ] Interface `LiquidationCommunaute` + `BienItem` dans `case-analysis.model.ts`
- [ ] Panneau visible si `liquidationCommunaute` non null
- [ ] 4 sous-tableaux (actif commun, biens propres A/B, passif commun)
- [ ] Valeur null affichée "—"
- [ ] Panneau absent si null
- [ ] Section PDF présente/absente selon champ
- [ ] 5 tests (3 composant + 2 PDF), 0 régression

---

## Périmètre

### Hors scope

- Calcul de la part de chaque époux
- Export Word

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale**

---

## Dépendances

- SF-FA-04-01 — statut : done ✅
