# SF-215-15 — Recours CCE extrême urgence 5 jours ouvrables — backend

## Identifiant
`F-215 / SF-215-15`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-15-cce-extreme-urgence-5j-be-backend`

---

## Objectif
Livrer le Calculator + Service + Entity + Endpoint backend pour `F-IM-32-cce-extreme-urgence-5j-be` : calculateur de délais du recours en extrême urgence devant le CCE — 5 jours **ouvrables** (BelgianBusinessDays) depuis l'acte exécutoire imminent (art. 39/82 §4 al. 2-3 Loi 15/12/1980), BELGIQUE UNIQUEMENT. Réutilise `BelgianBusinessDaysCalculator` déjà livré par F-IM-08-annexe13-be.

---

## Comportement attendu

### Cas nominal
- POST `/api/v1/case-files/{caseFileId}/cce-extreme-urgence-be-analysis`
- Body :
  - `dateActeExecutoire` (LocalDate, requis — date de l'OQT exécutoire ou du transfert Dublin imminent)
  - `typeActe` (enum : OQT_EXECUTE / TRANSFERT_DUBLIN / REFUS_ACCES_TERRITOIRE / EXPULSION_IMMEDIATE / AUTRE, requis)
  - `recoursForme` (Boolean, requis)
  - `dateRecours` (LocalDate, optionnel)
- `CceExtremeUrgenceBeCalculator` calcule (en jours **ouvrables BE**) :
  - `dateLimiteRecours` = `BelgianBusinessDaysCalculator.addBusinessDays(dateActeExecutoire, 5)`
  - `joursOuvrables Restants` = `BelgianBusinessDaysCalculator.countBusinessDays(today, dateLimiteRecours)`
  - `statut` ∈ { DISPONIBLE (joursOuvrables Restants > 2), CRITIQUE (joursOuvrables Restants ∈ [1, 2]), EXPIRE, RECOURS_FORME }
  - `audienceEstimee` = dateLimiteRecours + 1-2 jours (audience CCE souvent dans les 48h après dépôt)
  - `actionImmediate` : message d'action urgente si CRITIQUE ou EXPIRE

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| dateActeExecutoire future > 7 jours | Acte non encore imminent | 400 |
| recoursForme=true sans dateRecours | 400 | 400 |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `BelgianBusinessDaysCalculator` | Oui | Réutilisation directe — déjà livré dans `F-IM-08-annexe13-be`. Pattern identique. |
| SF-215-13 (annulation 30j) | Oui | F-IM-31 = annulation 30j calendaires ; F-IM-32 = extrême urgence 5j ouvrables. Deux cas procéduraux distincts. Documenter : si statut F-IM-31 = EXPIRE → basculer vers F-IM-32 si acte exécutoire imminent. |
| `F-IM-08-annexe13-be` | Oui | F-IM-08 couvre déjà les délais CCE en sous-partie (30j + 5j). F-IM-32 = outil dédié extrême urgence, plus détaillé et avec statut CRITIQUE. Complémentaires. |

---

## Conformité F-IA-04
- [ ] **Non applicable** — SF backend pure. Composant : SF-215-16.

---

## Champs IA à extraire (pré-remplissage)

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|-------|------|-----------------------------------------|-----------|
| `dateActeExecutoire` | date | `recoursExtremeUrgenceDateActe` | Nouveau |
| `typeActe` | enum | `recoursExtremeUrgenceTypeActe` | Nouveau — whitelist 5 valeurs |

2 champs réels. `recoursForme` / `dateRecours` aspirationnels.

---

## Critères d'acceptation

- [ ] `dateLimiteRecours` = +5 jours ouvrables BE depuis `dateActeExecutoire`
- [ ] `statut = CRITIQUE` si joursOuvrablesRestants ∈ [1, 2]
- [ ] Réutilisation `BelgianBusinessDaysCalculator` (pas de duplication)
- [ ] POST workspace FR → 400
- [ ] UT Calculator : ≥ 8 cas (DISPONIBLE, CRITIQUE, EXPIRE, RECOURS_FORME, week-end, jours fériés BE)
- [ ] IT Controller : ≥ 6 tests
- [ ] `F-IM-32-cce-extreme-urgence-5j-be` dans `KNOWN_FRONTEND_TOOL_IDS`
- [ ] Migration : table `cce_extreme_urgence_be_analyses` + visibility CONTEXTUAL (`recours_cce_extreme_urgence=true`)

---

## Hors périmètre
- Composant Angular (SF-215-16)
- Recours CCE annulation (SF-215-13/14)
- Génération document requête (F-IM-06)

---

## Plan de test
- `CceExtremeUrgenceBeCalculatorTest` : ≥ 8 cas dont jours fériés BE (Fête nationale 21/07, Toussaint, Noël, Armistice, etc.)
- `CceExtremeUrgenceBeControllerIT`

## Notes et décisions
- Source : Loi 15/12/1980 art. 39/82 §4 al. 2-3 ; loi 15/09/2006 ; AR 11/06/2018.
- Jours fériés BE : 1/01, 01/05, 21/07, 15/08, 01/11, 11/11, 25/12 + Pâques/Lundi Pâques/Ascension/Pentecôte (variables). Liste officielle dans `BelgianBusinessDaysCalculator`.
- Cas d'usage typique : OQT exécutoire imminent (rapatriement dans 2 jours) → recours en extrême urgence + suspension dans les 5 jours.
