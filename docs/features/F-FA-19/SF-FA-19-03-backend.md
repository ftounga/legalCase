# Mini-spec — F-FA-19 / SF-FA-19-03 Changement de résidence — déménagement parent (art. 373-2 al. 3 Cciv) — BACKEND

## Objectif

Calculateur d'**acceptabilité d'un changement de résidence de l'enfant suite au déménagement d'un parent** (art. 373-2 al. 3 Cciv). 2ème SF backend de F-FA-19 (1ère = SF-FA-19-01 autorité parentale exercice). Évalue la probabilité d'acceptation par le JAF d'un changement de résidence en fonction du respect de l'obligation d'information préalable, de la distance, de la raison du déménagement, du consentement de l'autre parent et de l'impact sur la scolarité.

Outil **single-country FR DROIT_FAMILLE**. Le droit belge a sa propre logique (art. 374 Code civil BE / loi du 18 juillet 2006) — couvert plus tard par une SF jumelle si pertinent.

## Comportement nominal

À partir d'une date de changement, d'une distance, d'une raison, d'un mode de résidence actuel et d'âges d'enfants :
- score 0-100 calé sur l'acceptabilité de la demande
- verdict de probabilité d'acceptation (ELEVEE/MOYENNE/FAIBLE)
- vérification du respect de l'obligation d'information préalable (art. 373-2 al. 3 Cciv : tout changement de résidence d'un parent, dès lors qu'il modifie les modalités d'exercice de l'autorité parentale, doit faire l'objet d'une information préalable et en temps utile à l'autre parent)
- recommandation d'expertise psychologique si distance > 300 km ou enfants > 10 ans
- vérification du délai de préavis légal (≥ 30 jours)
- messages structurés (notification 30j, art. 373-2, modification DVH, etc.)
- base juridique + formule explicative

### Algorithme de scoring

Score = somme des contributions (plafonné à 100) :
- information préalable + délai ≥ 30j (obligation art. 373-2) : **+30**
- raison reconnue (≠ AUTRE) : **+20**
- distance < 100 km : **+20** ; 100-300 km : **+10** ; > 300 km : **0**
- consentement de l'autre parent : **+20**
- pas d'impact scolarité majeur : **+10**

Verdicts :
- ELEVEE ≥ 70
- MOYENNE 40-69
- FAIBLE < 40

### Indicateurs dérivés

- `expertisePsyEnfantRecommandee` = true si `distanceKm > 300` OU si tout enfant a un âge > 10 ans
- `delaiPreavisLegalOk` = `informePrealablement && delaiInformationJours >= 30`
- `obligationInformationRespectee` = `informePrealablement && delaiInformationJours >= 30`

## Cas d'erreur

- `dateChangementPrevu` null → 400
- `distanceKm` null ou < 0 → 400
- `raisonChangement` invalide ou null → 400
- `modeResidenceActuel` invalide ou null → 400
- `delaiInformationJours` null ou < 0 → 400
- `ageEnfants` contient une valeur < 0 ou > 17 → 400
- Dossier non FRANCE → 400 ("Procédure propre au droit français — F-FA-19 BE non couverte")
- Dossier non DROIT_FAMILLE → 400
- Cross-workspace access → 404

## Critères d'acceptation

1. POST `/api/v1/case-files/{caseFileId}/changement-residence` avec info préalable 30j, raison `TRAVAIL`, distance 250 km, mode `ALTERNEE`, scolarité impactée → score 60 (30+20+10+0+0), verdict `MOYENNE`.
2. POST avec info préalable 30j + raison `TRAVAIL` + distance 80 km + consentement + pas d'impact scolarité → score 100, verdict `ELEVEE`.
3. POST avec pas d'info préalable + raison `AUTRE` + distance 500 km + pas de consentement + impact scolarité → score 0, verdict `FAIBLE`.
4. POST avec distance 350 km → `expertisePsyEnfantRecommandee = true`.
5. POST avec enfants âge [11, 13] → `expertisePsyEnfantRecommandee = true`.
6. POST avec distance 100 km, enfants [5, 7] → `expertisePsyEnfantRecommandee = false`.
7. POST avec `informePrealablement = false` → `obligationInformationRespectee = false`, message d'alerte sur l'obligation art. 373-2.
8. POST avec délai 20 jours → `delaiPreavisLegalOk = false`.
9. POST avec workspace BE → 400.
10. POST avec workspace DROIT_DU_TRAVAIL → 400.
11. GET après POST → renvoie l'analyse persistée.
12. GET sans POST préalable → 404.
13. Cross-workspace access (auth FR sur dossier BE) → 404.
14. Upsert : 2 POST successifs sur le même dossier → un seul enregistrement, dernier résultat retourné.

## Plan de test minimal

- **≥ 14 UT** sur `ChangementResidenceCalculator` :
  - score sur les 5 contributions (info préalable, raison, distance, consentement, scolarité)
  - distance par paliers (< 100, 100-300, > 300)
  - verdict aux trois seuils (ELEVEE / MOYENNE / FAIBLE)
  - expertise recommandée si distance > 300 km
  - expertise recommandée si tous les enfants > 10 ans
  - obligation information non respectée si pas d'info ou délai < 30j
  - throws sur raison invalide, mode invalide, distance négative, age invalide, dateChangement null
  - score plafonné à 100
- **≥ 8 IT** sur `ChangementResidenceControllerIT` :
  - POST nominal FR ELEVEE, POST FAIBLE, GET après POST, GET sans POST → 404, upsert, BE → 400, DT → 400, cross-WS → 404, raison invalide → 400

## Tables / endpoints / composants impactés

### Endpoints
- POST `/api/v1/case-files/{caseFileId}/changement-residence`
- GET `/api/v1/case-files/{caseFileId}/changement-residence`

### Tables
- `changement_residence_analyses` (nouvelle, migration **144**)
  - `id` UUID PK
  - `case_file_id` UUID FK unique → case_files
  - `date_changement_prevu` date NOT NULL
  - `distance_km` integer NOT NULL
  - `raison_changement` varchar(40) NOT NULL
  - `consentement_autre_parent` boolean NOT NULL
  - `informe_prealablement` boolean NOT NULL
  - `delai_information_jours` integer NOT NULL
  - `mode_residence_actuel` varchar(40) NOT NULL
  - `scolarite_impactee` boolean NOT NULL
  - `modification_dvh_demandee` boolean NOT NULL
  - `country` varchar(20) NOT NULL
  - `result_data` text NOT NULL (sérialisation `ChangementResidenceResult`)
  - `created_at`, `updated_at` timestamptz NOT NULL

### Visibility rule
- UUID `f1a04001-0000-0000-0000-ee00000fa193`
- Layer ALWAYS_ON, FRANCE + DROIT_FAMILLE
- Tool id `F-FA-19-changement-residence`
- Priority `78`

### Composants Java
- `ChangementResidenceCalculator` (algorithme art. 373-2 al. 3 Cciv)
- `ChangementResidenceRequest` / `ChangementResidenceResponse` / `ChangementResidenceResult` (records)
- `ChangementResidenceAnalysis` (entity)
- `ChangementResidenceRepository` (Spring Data)
- `ChangementResidenceService` (orchestration + persistance + gates)
- `ChangementResidenceController` (POST + GET)

## Hors périmètre

- **Frontend** (SF-FA-19-04 livrée en parallèle avec contrat figé)
- Régime BE équivalent (art. 374 Code civil BE / loi 18 juillet 2006 ; à traiter dans une SF jumelle si retenu après V7)
- Désaccords parentaux art. 373-2-10 (scolarité, santé, religion) — couvert par une SF ultérieure
- Modification du DVH consécutive (calcul des nouveaux trajets, jours de garde) — hors V1
- Génération du courrier d'information à l'autre parent (pas de PDF/template ici, voir F-FA-04)
- Calcul d'une éventuelle révision de pension alimentaire (voir F-FA-15)

## Analyse de cohérence transversale

- **Auth / Principal** : aucune modification — réutilise `OidcUser + Principal + CurrentUserResolver`
- **Workspace context** : aucune modification — gate `workspace.country == "FRANCE"` + `caseFile.legalDomain == "DROIT_FAMILLE"`, aligné sur F-FA-12/13/14/15/19-01
- **Plans / limites** : aucun gate plan — outil ALWAYS_ON
- **Navigation / routing** : aucune (frontend SF-FA-19-04)
- **Outil décisionnel métier** : nouvel outil. Scan effectué sur les outils décisionnels famille existants (F-FA-08/09/10/11/12/13/14/15 + F-FA-19-01 autorité parentale) — chacun couvre une situation distincte. Le changement de résidence (déménagement parent) est un sujet **distinct** non couvert par F-FA-19-01 (qui porte sur le régime d'exercice). Aucun chevauchement, classement = nouvel outil dédié, pas de scission ni d'extension. Pattern miroir SF-FA-19-01 (structure identique, scoring distinct).

## Nouveau pattern UI ou service partagé

- Aucun composant partagé / DTO réutilisable / endpoint transversal introduit. Contrat strictement scoped à `/api/v1/case-files/{id}/changement-residence`. Réutilisation des patterns existants (records DTO, entity 1:1 par dossier, service `@Transactional`, calculator pur, visibility rule ALWAYS_ON).

## Impact par domaine métier

- **DROIT_DU_TRAVAIL** : non applicable (sujet famille).
- **DROIT_FAMILLE** : cœur de la SF.
- **IMMIGRATION** : non applicable.
- **FRANCE** : couvert (art. 373-2 al. 3 Cciv).
- **BELGIQUE** : non couvert dans cette SF (gate strict 400). Loi 18 juillet 2006 BE applicable — à proposer comme SF jumelle si V7 confirme l'intérêt produit.

## Parité des domaines métier

Cet outil est de **niveau 5** (scoring d'acceptabilité). Vérification de l'équivalence sur les 2 autres domaines :
- **DROIT_DU_TRAVAIL** : concept non transposable (changement de résidence enfant = sujet famille).
- **IMMIGRATION** : concept non transposable (changement de résidence enfant = sujet famille).

L'outil est **intrinsèquement domain-specific** ; aucune feature jumelle requise.

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/changement-residence`

Request :
```json
{
  "dateChangementPrevu": "2026-09-01",
  "distanceKm": 250,
  "raisonChangement": "TRAVAIL",
  "consentementAutreParent": false,
  "informePrealablement": true,
  "delaiInformationJours": 30,
  "modeResidenceActuel": "ALTERNEE",
  "ageEnfants": [8, 12],
  "scolariteImpactee": true,
  "modificationDvhDemandee": true
}
```

Enums :
- `raisonChangement` : `TRAVAIL`, `FAMILLE`, `LOGEMENT`, `RAPPROCHEMENT_FAMILIAL`, `AUTRE`
- `modeResidenceActuel` : `ALTERNEE`, `EXCLUSIVE_DEMANDEUR`, `EXCLUSIVE_DEFENDEUR`

Response 200 :
```json
{
  "caseFileId": "uuid",
  "dateChangementPrevu": "2026-09-01",
  "distanceKm": 250,
  "raisonChangement": "TRAVAIL",
  "consentementAutreParent": false,
  "informePrealablement": true,
  "delaiInformationJours": 30,
  "modeResidenceActuel": "ALTERNEE",
  "ageEnfants": [8, 12],
  "scolariteImpactee": true,
  "modificationDvhDemandee": true,
  "scoreAcceptabilite": 60,
  "verdictProbabiliteAcceptation": "MOYENNE",
  "obligationInformationRespectee": true,
  "expertisePsyEnfantRecommandee": false,
  "delaiPreavisLegalOk": true,
  "baseJuridique": "Art. 373-2 al. 3 Cciv + jurisprudence Cass. 1ère civ.",
  "formule": "Distance 250 km + impact scolarité + résidence alternée → score 60 = acceptabilité MOYENNE",
  "messages": ["Notification 30j avant déménagement obligatoire (art. 373-2)", "..."],
  "country": "FRANCE"
}
```

Codes erreur :
- `400` : validation (raison/mode invalide, distance négative, age invalide, dossier BE, dossier DT, payload manquant)
- `404` : dossier inexistant ou cross-workspace
