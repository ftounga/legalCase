# Mini-spec — F-DT-05 / SF-DT-05-02 Calculateur indemnités de préavis belge

---

## Identifiant

`F-DT-05 / SF-DT-05-02`

## Feature parente

`F-DT-05` — Droit du travail belge — types de litige, prescription et indemnités de préavis

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-DT-05-02-indemnites-belgique`

---

## Objectif

Implémenter le calculateur d'indemnités de préavis belge (table ancienneté → semaines, indemnité compensatoire, fourchette CCT 109) et l'afficher dans le panneau Synthèse quand le workspace est BELGIQUE.

---

## Comportement attendu

### Cas nominal

1. L'IA extrait `compensation_data` (ancienneté, salaire) depuis les documents d'un dossier belge
2. Le `BelgianCompensationCalculator` calcule :
   - Le préavis en semaines selon la table d'ancienneté (Loi du 26 décembre 2013)
   - L'indemnité compensatoire = salaire hebdomadaire brut × semaines de préavis
   - La fourchette CCT 109 = 3 à 17 semaines de salaire (si licenciement manifestement déraisonnable)
3. Le panneau indemnités dans la synthèse affiche les résultats adaptés au droit belge
4. L'export PDF inclut les indemnités belges

### Table de préavis belge (Loi du 26 décembre 2013, art. 37/2)

| Ancienneté | Préavis (semaines) |
|------------|-------------------|
| 0 à < 3 mois | 1 |
| 3 à < 6 mois | 3 |
| 6 à < 9 mois | 4 |
| 9 à < 12 mois | 5 |
| 12 à < 15 mois | 6 |
| 15 à < 18 mois | 7 |
| 18 à < 21 mois | 8 |
| 21 à < 24 mois | 9 |
| 2 à < 3 ans | 10 |
| 3 à < 4 ans | 12 |
| 4 à < 5 ans | 13 |
| 5 à 20 ans | 15 + 3 par année au-delà de 5 |
| > 20 ans | 62 + 1 par année au-delà de 20 |

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Données manquantes (salaire null) | donneesPartielles=true, calcul partiel |
| Ancienneté = 0 | Préavis = 1 semaine |
| Workspace FRANCE | Calculateur français (Macron) utilisé, pas le belge |

---

## Critères d'acceptation

- [ ] `BelgianCompensationCalculator` créé avec la table de préavis complète
- [ ] Calcul : préavis semaines + indemnité compensatoire + fourchette CCT 109
- [ ] `CaseAnalysisResponse.extractCompensationEstimate` dispatch selon le country du workspace
- [ ] Le panneau indemnités affiche "Préavis légal" + "Indemnité compensatoire" + "Fourchette CCT 109" pour la Belgique
- [ ] L'export PDF inclut les indemnités belges
- [ ] Tests unitaires du calculateur belge (10+ cas d'ancienneté)
- [ ] Les données sont configurées pour FRANCE et BELGIQUE
- [ ] Tous les tests existants restent verts

---

## Périmètre

### Hors scope

- Calcul en deux parties (droits acquis avant/après 2014) — trop complexe pour V1
- Protections spéciales (délégué du personnel, travailleuse enceinte) — indemnités forfaitaires séparées

---

## Technique

### Fichiers créés

| Fichier | Description |
|---------|-------------|
| `BelgianCompensationCalculator.java` | Calcul préavis + indemnité + CCT 109 |

### Fichiers modifiés

| Fichier | Modification |
|---------|-------------|
| `CaseAnalysisResponse.java` | Dispatch extractCompensationEstimate selon country |
| `synthesis.component.html` | Affichage adapté Belgique (préavis semaines, CCT 109) |
| `pdf-export.service.ts` | Section indemnités belge dans le PDF |

---

## Plan de test

### Tests unitaires

- [ ] BelgianCompensationCalculator — 0 mois → 1 semaine
- [ ] BelgianCompensationCalculator — 4 mois → 3 semaines
- [ ] BelgianCompensationCalculator — 7 mois → 4 semaines
- [ ] BelgianCompensationCalculator — 2 ans → 10 semaines
- [ ] BelgianCompensationCalculator — 5 ans → 15 semaines
- [ ] BelgianCompensationCalculator — 10 ans → 30 semaines
- [ ] BelgianCompensationCalculator — 20 ans → 62 semaines
- [ ] BelgianCompensationCalculator — 25 ans → 67 semaines
- [ ] BelgianCompensationCalculator — indemnité compensatoire = salaire hebdomadaire × semaines
- [ ] BelgianCompensationCalculator — fourchette CCT 109 = 3 à 17 semaines de salaire
- [ ] Tous tests existants verts

## Analyse d'impact

- [x] **Aucune préoccupation transversale** — ajout d'un calculateur et adaptation affichage
