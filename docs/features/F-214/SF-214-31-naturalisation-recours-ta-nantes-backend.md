# Mini-spec — F-214 / SF-214-31 — Recours refus naturalisation TA Nantes (décret) — backend

## Identifiant

`F-214 / SF-214-31`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Calculer les délais de recours administratif devant le TA de Nantes contre un refus de naturalisation par décret (voie Cciv 21-15/21-17), juridiction compétente exclusivement pour les refus de naturalisation par décret.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/naturalisation-recours-ta-analysis`
- Body : `dateRefusDecret` (LocalDate, requis), `motivationRefus` (string, optionnel, ≤ 500), `recoursPrerequis` (boolean — recours préalable obligatoire ministre si > 18 mois attente)
- Calculator `NaturalisationRecoursTableauCalculator` :
  - `dateEcheanceRecoursTa` = dateRefusDecret + 2 mois (CJA droit commun)
  - `joursRestants` = dateEcheanceRecoursTa - today
  - `tribunalCompetent` : TA Nantes (compétence exclusive nationale refus décret naturalisation)
  - `basesJuridiques` : CJA L. 213-1 + Cciv 21-15
  - `motifsRecoursDisponibles` : liste (défaut de motivation, excès de pouvoir, erreur appréciation critères intégration, vice de procédure)
  - `statut` ∈ {`RECOURS_POSSIBLE`, `URGENT` (< 15 j), `PRESCRIT`}
- Output persisté dans `naturalisation_recours_ta_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/naturalisation-recours-ta-analysis` → 200 ou 404

---

## Source juridique

- **Cciv 21-15 à Cciv 21-27** — naturalisation par décret.
- **CJA L. 213-1 / R. 312-4** — compétence exclusive TA Nantes pour refus naturalisation décret.
- **CE 11 mars 2009, n° 303597** (à vérifier) — motifs d'annulation refus décret.
- **CE 27 novembre 2013, n° 358734** — motivation refus naturalisation.

**Distinction** : SF-214-29 = recours TJ pour refus DÉCLARATION (voies mariage/ascendant/mineur). SF-214-31 = recours TA Nantes pour refus DÉCRET (naturalisation ordinaire 5 ans résidence).

---

## Champs IA à extraire

Réutilise `naturalisationVoie` + `naturalisationDateRefus` introduits par SF-214-29.

**Trigger CONTEXTUAL** : `naturalisationEnvisageeDetectee` (existant F-201).

---

## Critères d'acceptation

- [x] POST retourne tribunalCompetent = TA Nantes, delai 2 mois
- [x] POST PRESCRIT → 200 + statut PRESCRIT + message
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-40-naturalisation-recours-ta-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`naturalisation_envisagee_detectee`

## Plan de test minimal

- **UT** `NaturalisationRecoursTableauCalculatorTest` : 5+ cas
- **IT** `NaturalisationRecoursTableauControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `naturalisation_recours_ta_analyses`
- **Migration Liquibase** + seed visibility rules
- Pas d'extension `ImmigrationExtractedData` supplémentaire (champs SF-214-29 suffisants)
- **Endpoint** `NaturalisationRecoursTableauController`

## Hors périmètre

- Composant Angular (SF-214-32)
