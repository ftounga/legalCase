# Mini-spec — F-FA-02 / SF-FA-02-02 Pension alimentaire — frontend panneau + export PDF

## Identifiant

`F-FA-02 / SF-FA-02-02`

## Feature parente

`F-FA-02` — Grille pension alimentaire

## Statut

`in-progress`

## Date de création

2026-04-04

## Branche Git

`feat/SF-FA-02-02-pension-alimentaire-frontend`

---

## Objectif

Afficher `pensionAlimentaireEstimate` en panneau lecture seule dans `SynthesisComponent` si l'estimate est non null, et inclure la section "Pension alimentaire indicative" dans l'export PDF.

---

## Comportement attendu

### Cas nominal

1. `SynthesisComponent` reçoit `synthesis.pensionAlimentaireEstimate` non null.
2. Un panneau `<mat-expansion-panel>` "Pension alimentaire indicative" s'affiche avec :
   - Fourchette : `{montantMin} € – {montantMax} € / mois`
   - Revenus net débiteur, barème (UNAF France / CGKR Belgique)
   - Badge `⚠ Données partielles` si `donneesPartielles = true`
   - Avertissement légal : "Estimation indicative — ne constitue pas un avis juridique"
3. Export PDF : section présente si `synthesis.pensionAlimentaireEstimate` non null.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `pensionAlimentaireEstimate` null / absent | Panneau absent, section PDF absente |

---

## Critères d'acceptation

- [ ] Panneau visible si `pensionAlimentaireEstimate` non null
- [ ] Fourchette min/max affichée en euros
- [ ] Badge données partielles conditionnel
- [ ] Avertissement légal présent
- [ ] Panneau absent si estimate null
- [ ] Export PDF : section présente si estimate non null, absente sinon
- [ ] Interface `PensionAlimentaireEstimate` ajoutée dans `case-analysis.model.ts`

---

## Périmètre

### Hors scope

- Formulaire de saisie manuelle
- Persistance en DB
- Export Word

---

## Technique

### Composants modifiés

| Composant | Modification |
|-----------|-------------|
| `case-analysis.model.ts` | Ajout `PensionAlimentaireEstimate` + champ dans `CaseAnalysisResult` |
| `SynthesisComponent` | Import + getter `pensionAlimentaireEstimate` + panneau HTML |
| `PdfExportService` | `buildPensionAlimentaireSection()` + appel dans `buildSections()` |

---

## Plan de test

### Tests unitaires

- [ ] `SynthesisComponent` — panneau visible si `pensionAlimentaireEstimate` non null
- [ ] `SynthesisComponent` — panneau absent si `pensionAlimentaireEstimate` null
- [ ] `SynthesisComponent` — badge données partielles conditionnel
- [ ] `PdfExportService.buildDocument()` — section pension présente si estimate non null
- [ ] `PdfExportService.buildDocument()` — section pension absente si estimate null

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — extension isolée de SynthesisComponent et PdfExportService

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

- SF-FA-02-01 — statut : done ✅
