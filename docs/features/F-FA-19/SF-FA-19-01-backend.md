# Mini-spec — F-FA-19 / SF-FA-19-01 Autorité parentale — exercice (art. 372-373 Cciv) — BACKEND

## Objectif

Calculateur d'**éligibilité au changement de régime d'exercice de l'autorité parentale** (art. 372-373 Cciv). Premier morceau de F-FA-19. Évalue la probabilité d'acceptation par le JAF d'une demande de passage de l'exercice conjoint à un exercice exclusif (mère, père) ou à une délégation à un tiers, en fonction du motif invoqué, des preuves produites et des circonstances (danger caractérisé, ingérence dans la vie de l'enfant).

Outil **single-country FR DROIT_FAMILLE**. Le droit belge a sa propre logique (autorité parentale conjointe par défaut, art. 373 Code civil BE) — couvert plus tard par une SF jumelle si pertinent.

## Comportement nominal

À partir d'un régime actuel et d'un régime demandé, d'un motif et de preuves :
- score 0-100 calé sur la solidité de la demande
- verdict de probabilité d'acceptation (ELEVEE/MOYENNE/FAIBLE)
- recommandation d'expertise psychiatrique si motif sensible (mise en danger, aliénation parentale)
- recommandation d'audition des enfants ≥ 13 ans (art. 388-1 Cciv)
- messages structurés (audition obligatoire ≥ 13 ans, charge de la preuve, etc.)
- base juridique + formule explicative

### Algorithme de scoring

Score = somme des contributions (plafonné à 100) :
- motifChangement reconnu (≠ AUTRE) : **+25**
- ≥ 2 preuves produites : **+25**
- danger caractérisé : **+25**
- interférence dans la vie de l'enfant : **+15**
- consentement de l'autre parent : **+10**

Verdicts :
- ELEVEE ≥ 70
- MOYENNE 40-69
- FAIBLE < 40

### Recommandations dérivées

- `expertiseRecommandee` = true si `motifChangement ∈ {MISE_EN_DANGER, ALIENATION_PARENTALE}`
- `auditionEnfantsRecommandee` = true si tout enfant a un âge ≥ 13 ans (art. 388-1)

## Cas d'erreur

- `regimeExerciceActuel` ou `regimeExerciceDemande` invalide ou null → 400
- `regimeExerciceActuel == regimeExerciceDemande` → 400 (aucun changement à évaluer)
- `motifChangement` invalide → 400
- `preuvesProduites` contient un code inconnu → 400
- `ageEnfants` contient une valeur < 0 ou > 17 → 400
- `dateRequete` null → 400
- Dossier non FRANCE → 400 ("Procédure propre au droit français — F-FA-19 BE non couverte")
- Dossier non DROIT_FAMILLE → 400
- Cross-workspace access → 404

## Critères d'acceptation

1. POST `/api/v1/case-files/{caseFileId}/autorite-parentale` avec motif `MISE_EN_DANGER`, 2 preuves, danger caractérisé → score 75, verdict `ELEVEE`, expertise recommandée.
2. POST avec motif `AUTRE`, 0 preuve, pas de danger → score 0, verdict `FAIBLE`.
3. POST avec un enfant de 13 ans → `auditionEnfantsRecommandee = true`.
4. POST avec un enfant de 12 ans et un de 14 ans → `auditionEnfantsRecommandee = false` (la garantie est sur tous).
5. POST avec motif `ALIENATION_PARENTALE` → `expertiseRecommandee = true`.
6. POST avec motif `DESINTERET_PROLONGE` (motif reconnu non sensible) → `expertiseRecommandee = false`.
7. POST avec consentement de l'autre parent → +10 pris en compte dans le score.
8. POST avec `regimeActuel == regimeDemande` → 400.
9. POST avec workspace BE → 400.
10. POST avec workspace DROIT_DU_TRAVAIL → 400.
11. GET après POST → renvoie l'analyse persistée.
12. GET sans POST préalable → 404.
13. Cross-workspace access (auth FR sur dossier BE) → 404.
14. Upsert : 2 POST successifs sur le même dossier → un seul enregistrement, dernier résultat retourné.

## Plan de test minimal

- **≥ 14 UT** sur `AutoriteParentaleCalculator` :
  - score sur les 5 contributions (motif, preuves, danger, ingérence, consentement)
  - verdict aux trois seuils (ELEVEE / MOYENNE / FAIBLE)
  - expertise recommandée pour MISE_EN_DANGER et ALIENATION_PARENTALE
  - audition recommandée si tous les enfants ≥ 13 ans
  - audition non recommandée si un enfant < 13 ans
  - throws sur regime invalide, motif invalide, preuve invalide, age invalide, regime identique
  - score plafonné à 100
- **≥ 8 IT** sur `AutoriteParentaleControllerIT` :
  - POST nominal FR ELEVEE, POST FAIBLE, GET après POST, GET sans POST → 404, upsert, BE → 400, DT → 400, cross-WS → 404, regime identique → 400, motif invalide → 400

## Tables / endpoints / composants impactés

### Endpoints
- POST `/api/v1/case-files/{caseFileId}/autorite-parentale`
- GET `/api/v1/case-files/{caseFileId}/autorite-parentale`

### Tables
- `autorite_parentale_analyses` (nouvelle, migration **139**)
  - `id` UUID PK
  - `case_file_id` UUID FK unique → case_files
  - `regime_exercice_actuel` varchar(40) NOT NULL
  - `regime_exercice_demande` varchar(40) NOT NULL
  - `motif_changement` varchar(40) NOT NULL
  - `danger_caracterise` boolean NOT NULL
  - `consentement_autre_parent` boolean NOT NULL
  - `interference_vie_enfant` boolean NOT NULL
  - `date_requete` date NOT NULL
  - `country` varchar(20) NOT NULL
  - `result_data` text NOT NULL (sérialisation `AutoriteParentaleResult`)
  - `created_at`, `updated_at` timestamptz NOT NULL

### Visibility rule
- UUID `f1a04001-0000-0000-0000-ee00000fa191`
- Layer ALWAYS_ON, FRANCE + DROIT_FAMILLE
- Tool id `F-FA-19-autorite-parentale`
- Priority `77`

### Composants Java
- `AutoriteParentaleCalculator` (algorithme art. 372-373 Cciv)
- `AutoriteParentaleRequest` / `AutoriteParentaleResponse` / `AutoriteParentaleResult` (records)
- `AutoriteParentaleAnalysis` (entity)
- `AutoriteParentaleRepository` (Spring Data)
- `AutoriteParentaleService` (orchestration + persistance + gates)
- `AutoriteParentaleController` (POST + GET)

## Hors périmètre

- **Frontend** (SF-FA-19-02 livrée en parallèle avec contrat figé)
- Régime BE équivalent (loi 13 avril 1995 ; à traiter dans une SF jumelle si retenu après V7)
- Désaccords parentaux art. 373-2-10 (scolarité, santé, religion) — couvert par une SF ultérieure
- Droit de visite médiatisé / relations avec tiers (grand-parent art. 371-4) — couvert par une SF ultérieure
- Changement de résidence (déménagement d'un parent) — couvert par une SF ultérieure
- Génération de pièces à joindre (pas de PDF/template ici, voir F-FA-04)
- Calcul d'une pension alimentaire associée (voir F-FA-02)

## Analyse de cohérence transversale

- **Auth / Principal** : aucune modification — réutilise `OidcUser + Principal + CurrentUserResolver`
- **Workspace context** : aucune modification — gate `workspace.country == "FRANCE"` + `caseFile.legalDomain == "DROIT_FAMILLE"`, aligné sur F-FA-12/13/15
- **Plans / limites** : aucun gate plan — outil ALWAYS_ON
- **Navigation / routing** : aucune (frontend SF-FA-19-02)
- **Outil décisionnel métier** : nouvel outil. Scan effectué sur les outils décisionnels famille existants (F-FA-08/09/10/11/12/13/14/15) — chacun couvre une situation distincte (divorce altération, divorce faute, divorce accepté, désunion irrémédiable BE, mesures provisoires AOMP, révisions post-divorce, ordonnance de protection, récompenses). L'autorité parentale est un sujet **distinct** non couvert : aucun chevauchement, classement = nouvel outil dédié, pas de scission ni d'extension.

## Nouveau pattern UI ou service partagé

- Aucun composant partagé / DTO réutilisable / endpoint transversal introduit. Contrat strictement scoped à `/api/v1/case-files/{id}/autorite-parentale`. Réutilisation des patterns existants (records DTO, entity 1:1 par dossier, service `@Transactional`, calculator pur, visibility rule ALWAYS_ON).

## Impact par domaine métier

- **DROIT_DU_TRAVAIL** : non applicable (sujet famille).
- **DROIT_FAMILLE** : cœur de la SF.
- **IMMIGRATION** : non applicable.
- **FRANCE** : couvert (art. 372-373 Cciv).
- **BELGIQUE** : non couvert dans cette SF (gate strict 400). Loi 13 avril 1995 BE applicable — à proposer comme SF jumelle si V7 confirme l'intérêt produit.

## Parité des domaines métier

Cet outil est de **niveau 5** (scoring d'éligibilité). Vérification de l'équivalence sur les 2 autres domaines :
- **DROIT_DU_TRAVAIL** : concept non transposable (autorité parentale = sujet famille).
- **IMMIGRATION** : concept non transposable (autorité parentale = sujet famille).

L'outil est **intrinsèquement domain-specific** ; aucune feature jumelle requise.

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/autorite-parentale`

Request :
```json
{
  "regimeExerciceActuel": "CONJOINT",
  "regimeExerciceDemande": "EXCLUSIF_MERE",
  "motifChangement": "MISE_EN_DANGER",
  "dangerCaracterise": true,
  "preuvesProduites": ["JUGEMENT_PENAL", "TEMOIGNAGES_ECOLE"],
  "ageEnfants": [8, 12],
  "consentementAutreParent": false,
  "interferenceVieEnfant": true,
  "dateRequete": "2026-05-10"
}
```

Enums :
- `regimeExerciceActuel` / `regimeExerciceDemande` : `CONJOINT`, `EXCLUSIF_MERE`, `EXCLUSIF_PERE`, `DELEGATION_TIERS`
- `motifChangement` : `MISE_EN_DANGER`, `DESINTERET_PROLONGE`, `IMPOSSIBILITE_FAIT`, `CONDAMNATION_PENALE`, `ALIENATION_PARENTALE`, `AUTRE`
- `preuvesProduites` (array) : `JUGEMENT_PENAL`, `MAINS_COURANTES`, `CERTIFICAT_MEDICAL`, `TEMOIGNAGES_ECOLE`, `TEMOIGNAGES_PROCHES`, `RAPPORT_AED`, `EXPERTISE_PSYCHIATRIQUE`, `AUTRE`

Response 200 :
```json
{
  "caseFileId": "uuid",
  "regimeExerciceActuel": "CONJOINT",
  "regimeExerciceDemande": "EXCLUSIF_MERE",
  "motifChangement": "MISE_EN_DANGER",
  "dangerCaracterise": true,
  "preuvesProduites": ["JUGEMENT_PENAL", "TEMOIGNAGES_ECOLE"],
  "ageEnfants": [8, 12],
  "consentementAutreParent": false,
  "interferenceVieEnfant": true,
  "dateRequete": "2026-05-10",
  "scoreEligibilite": 75,
  "verdictProbabiliteAcceptation": "ELEVEE",
  "expertiseRecommandee": true,
  "auditionEnfantsRecommandee": false,
  "baseJuridique": "Art. 372-373 Cciv + jurisprudence Cass. 1ère civ.",
  "formule": "Score 75 = motif caractérisé + 2 preuves variées + danger réel",
  "messages": ["..."],
  "country": "FRANCE"
}
```

Codes erreur :
- `400` : validation (regime/motif invalide, regimes identiques, age invalide, dossier BE, dossier DT, payload manquant)
- `404` : dossier inexistant ou cross-workspace
