# Mini-spec — F-207 / SF-207-06-backend Outil RCC BE — conditions d'éligibilité

## Identifiant

`F-207 / SF-207-06-backend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-06-backend-rcc-be-conditions`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern source : analyseur d'éligibilité (similaire à `RefereTribunalTravailBe*` SF-207-05) + pattern BE workspace gate (SF-207-01..05).

## Objectif

Analyseur d'éligibilité au **RCC** (Régime de Chômage avec Complément d'entreprise, ex-prépension BE) — détermine si le salarié remplit les conditions d'âge et carrière pour bénéficier du RCC, sous l'un des 4 régimes (général 60+/40, métiers lourds 58+/35, longue carrière 59+/40, entreprise en difficulté). Sources : CCT 17 ; AR 03/05/2007 ; CCT 17/13. Outil BE-only.

## Contrat API

`POST /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-conditions`

Inputs (`RccBeConditionsRequest`) :
```json
{
  "dateNaissance": "1966-03-15",                       // requis
  "anneesCarriereProfessionnelle": 41,                 // requis (carrière professionnelle salariée)
  "metierLourd": true,                                 // booleen
  "longueCarriere": false,                             // booleen (≥ 40 ans à la date considérée)
  "entrepriseEnDifficulte": false,                     // booleen
  "dateLicenciementEnvisagee": "2026-12-31"            // optionnel ; default today
}
```

Réponse 200 :
```json
{
  "verdict": "ELIGIBLE" | "INCERTAIN" | "NON_ELIGIBLE",
  "regimeApplicable": "GENERAL" | "METIERS_LOURDS" | "LONGUE_CARRIERE" | "ENTREPRISE_DIFFICULTE" | null,
  "regimesEligibles": ["GENERAL", "METIERS_LOURDS"],
  "ageALaDateLicenciement": 60,
  "anneesCarriereCalculees": 41,
  "conditionsManquantes": ["AGE_GENERAL_60", "..."],     // si verdict != ELIGIBLE
  "baseJuridique": "CCT 17 (régime général) ; CCT 17/13 (métiers lourds) ; AR 03/05/2007 art. 3 ; loi 03/07/1978",
  "formuleCalcul": "Âge à la date envisagée : 60 ans ; carrière : 41 ans ; régime GENERAL applicable (60+/40 ✓)."
}
```

Logique de calcul (`RccBeConditionsCalculator`) :

Vérification des 4 régimes (parallèles, le 1ᵉʳ qui matche → `regimeApplicable`) :

| Régime | Conditions |
|---|---|
| `GENERAL` (CCT 17 art. 3) | Âge ≥ 60 ans **ET** carrière ≥ 40 ans |
| `METIERS_LOURDS` (CCT 17/13) | Âge ≥ 58 ans **ET** carrière ≥ 35 ans **ET** `metierLourd=true` |
| `LONGUE_CARRIERE` (CCT 17 art. 3) | Âge ≥ 59 ans **ET** carrière ≥ 40 ans **ET** `longueCarriere=true` |
| `ENTREPRISE_DIFFICULTE` (AR 03/05/2007 art. 8) | Âge ≥ 60 ans (variable) **ET** `entrepriseEnDifficulte=true` |

Verdict :
- `ELIGIBLE` si ≥ 1 régime matche → `regimesEligibles` liste tous les régimes qui matchent ; `regimeApplicable` = le plus favorable (priorité GENERAL > LONGUE_CARRIERE > METIERS_LOURDS > ENTREPRISE_DIFFICULTE — *favorable* = stabilité juridique).
- `INCERTAIN` si âge ≥ 55 ans ET carrière ≥ 30 ans (proche des seuils, l'avocat doit affiner les données) → `regimesEligibles=[]`, `conditionsManquantes` détaille ce qui manque.
- `NON_ELIGIBLE` sinon → `regimesEligibles=[]`, `conditionsManquantes` détaille.

`ageALaDateLicenciement` calculé en années pleines selon `dateLicenciementEnvisagee` (default today, Europe/Brussels).

Persistance : 1 ligne `rcc_be_conditions_analyses` par dossier (unique sur `case_file_id`).

## Cas d'erreur

| Situation | Code |
|---|---|
| `workspaceCountry !== BELGIQUE` | 404 |
| `caseFileId` autre workspace | 404 |
| `dateNaissance` futur ou > 100 ans | 400 |
| `anneesCarriereProfessionnelle < 0` ou > 60 | 400 |
| `dateLicenciementEnvisagee < dateNaissance` | 400 |

## Composants à créer (pattern `RefereTribunalTravailBe*` SF-207-05)

Sous `backend/src/main/java/fr/ailegalcase/casefile/` :
- `RccBeConditionsAnalysis.java`
- `RccBeConditionsRepository.java`
- `RccBeConditionsRegime.java` — enum 4 valeurs.
- `RccBeConditionsCondition.java` — enum conditions manquantes (`AGE_GENERAL_60`, `CARRIERE_40`, `METIER_LOURD_FLAG`, `AGE_METIERS_LOURDS_58`, `CARRIERE_METIERS_LOURDS_35`, `LONGUE_CARRIERE_FLAG`, `ENTREPRISE_DIFFICULTE_FLAG`).
- `RccBeConditionsRequest.java` (Bean Validation).
- `RccBeConditionsResult.java` (record + enum `Verdict` 3 valeurs + `regimeApplicable` nullable + `regimesEligibles` List + `ageALaDateLicenciement` int + `anneesCarriereCalculees` int + `conditionsManquantes` List).
- `RccBeConditionsResponse.java`
- `RccBeConditionsCalculator.java` — 4 vérifications de régimes en parallèle + priorisation + détection INCERTAIN (proche des seuils).
- `RccBeConditionsService.java` (gate `BELGIQUE`, validation, persistance).
- `RccBeConditionsController.java` (POST + GET).

Migration `XXX-create-rcc-be-conditions-analyses.xml` (prochain après 261). Table standard. Rollback.

Extensions :
- `LegalDomainPromptBuilder` branche BE Travail : ajout 4 champs IA (`dateNaissanceSalarie` String, `anneesCarriereSalarie` Integer, `metierLourdDetecte` Boolean, `entrepriseEnDifficulteDetectee` Boolean) + 4 `critereCode` BE_RCC_DATE_NAISSANCE, BE_RCC_ANNEES_CARRIERE, BE_RCC_METIER_LOURD, BE_RCC_ENTREPRISE_DIFFICULTE.
- `CaseAnalysisResponse.TravailExtractedData` : ajout ces 4 fields (rétrocompat via Builder uniquement).

## Critères d'acceptation

- [ ] Âge ≥ 60 + carrière ≥ 40 → `ELIGIBLE` avec `regimeApplicable=GENERAL`.
- [ ] Âge 58 + carrière 35 + métier lourd → `ELIGIBLE` avec `regimeApplicable=METIERS_LOURDS`.
- [ ] Âge 60 + carrière 40 + métier lourd → `ELIGIBLE` avec `regimesEligibles=[GENERAL, METIERS_LOURDS]`, `regimeApplicable=GENERAL` (priorité).
- [ ] Âge 56 + carrière 32 → `INCERTAIN` (≥ 55 + ≥ 30) avec `conditionsManquantes` listée.
- [ ] Âge 50 + carrière 20 → `NON_ELIGIBLE`.
- [ ] `metierLourd=false` avec âge 58 et carrière 35 → métiers lourds NON applicable, `INCERTAIN` global.
- [ ] Workspace FR → 404 ; autre workspace → 404.
- [ ] `dateNaissance` futur → 400.
- [ ] `GET` après `POST` → 200.
- [ ] `critereCode` BE_RCC_* émis.

## Plan de test

`RccBeConditionsCalculatorTest` (12+ tests : 4 régimes × ELIGIBLE/INCERTAIN/NON_ELIGIBLE + cumul régimes + priorisation + bornes seuils).
`RccBeConditionsControllerIT` (5+ tests : BE OK, FR 404, autre workspace 404, validation 400, GET 404).

## Hors scope

- Frontend (SF-207-06b).
- Calcul indemnité complémentaire RCC (SF-207-07 — outil séparé, mémoire `feedback_decision_tools_one_per_situation`).
- RCC handicapés (régime spécifique non couvert V1).
- Conditions employeur (CP du salarié, secteurs éligibles) — version V2.

## Dépendances

- Pattern `RefereTribunalTravailBe*` (SF-207-05).
- Aucune dépendance directe sur les autres SF F-207.
