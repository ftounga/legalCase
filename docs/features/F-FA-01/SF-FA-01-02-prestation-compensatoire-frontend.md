# Mini-spec — F-FA-01 / SF-FA-01-02 Prestation compensatoire — frontend panneau + export PDF

## Identifiant

`F-FA-01 / SF-FA-01-02`

## Feature parente

`F-FA-01` — Calcul de la prestation compensatoire

## Statut

`in-progress`

## Date de création

2026-04-04

## Branche Git

`feat/SF-FA-01-02-prestation-compensatoire-frontend`

---

## Objectif

Afficher `prestationCompensatoireEstimate` en panneau lecture seule dans `SynthesisComponent` si non null, et inclure la section dans l'export PDF.

---

## Comportement attendu

### Cas nominal

1. `synthesis.prestationCompensatoireEstimate` non null → panneau "Prestation compensatoire indicative" visible
2. Fourchette capitale totale (montantMin – montantMax), écart revenus, barème, avertissement légal
3. Export PDF : section présente si estimate non null

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| `prestationCompensatoireEstimate` null | Panneau absent, section PDF absente |

---

## Critères d'acceptation

- [ ] Interface `PrestationCompensatoireEstimate` dans `case-analysis.model.ts`
- [ ] Getter `prestationCompensatoireEstimate` dans `SynthesisComponent`
- [ ] Panneau conditionnel avec fourchette, écart revenus, barème, avertissement
- [ ] Badge données partielles conditionnel
- [ ] Section PDF présente/absente selon estimate
- [ ] 5 tests (3 composant + 2 PDF), 0 régression

---

## Périmètre

### Hors scope

- Saisie manuelle, persistance, export Word

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale**

---

## Dépendances

- SF-FA-01-01 — statut : done ✅
