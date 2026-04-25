# Mini-spec — F-FA-19 / SF-FA-19-05 Désaccords parentaux (art. 373-2-10 Cciv) — BACKEND

## Objectif

Calculateur d'**éligibilité au recours JAF en cas de désaccord parental** (art. 373-2-10 Cciv) sur l'exercice de l'autorité parentale conjointe : scolarité (changement d'établissement, langues, redoublement), santé (vaccins, opérations non urgentes, suivi psychologique), religion (baptême, école confessionnelle), loisirs / sports (activités structurantes, déplacements à l'étranger), choix éducatifs (internat, sport intensif), déménagement, etc. Troisième morceau backend de F-FA-19. Évalue la probabilité d'acceptation par le JAF en fonction du domaine, de l'intensité du désaccord, des tentatives de médiation préalable, de l'âge des enfants concernés et des circonstances.

Outil **single-country FR DROIT_FAMILLE**. Le droit belge dispose d'une procédure équivalente (art. 374 §1er Code civil BE — désaccords parentaux portés devant le tribunal de la famille) couverte plus tard par une SF jumelle si pertinent.

## Comportement nominal

À partir d'un domaine de désaccord, d'une intensité, des tentatives de médiation et du contexte enfants :
- score 0-100 calé sur la solidité du recours
- verdict de probabilité d'acceptation (ELEVEE/MOYENNE/FAIBLE)
- recommandation médiation préalable si aucune tentative
- recommandation audition des enfants ≥ 13 ans (art. 388-1 Cciv)
- recommandation expertise psychologique si désaccord MAJEUR sans expertise déjà réalisée
- délai prévisionnel de traitement (90 jours nominal, 30 jours en référé urgent)
- messages structurés (saisine par requête simple, charge de la preuve, intérêt supérieur de l'enfant)
- base juridique + formule explicative

### Algorithme de scoring

Score = somme des contributions (plafonné à 100) :
- domaineDesaccord reconnu (≠ AUTRE) : **+20**
- intensiteDesaccord MAJEUR : **+30** ; MOYEN : **+15** ; MINEUR : **+5**
- tentativesMediation ≥ 2 : **+25** ; 1 (hors AUCUNE) : **+10** ; 0 / AUCUNE : 0
- interetSuperieurInvoque : **+15**
- urgence : **+10**

Verdicts :
- ELEVEE ≥ 70
- MOYENNE 40-69
- FAIBLE < 40

### Recommandations dérivées

- `mediationPrealableRecommandee` = `tentativesMediation` vide OU contient seulement `AUCUNE`
- `auditionEnfantsRecommandee` = au moins un enfant a un âge ≥ 13 ans (art. 388-1)
- `expertisePsyRecommandee` = `intensiteDesaccord == MAJEUR && !expertiseDejaRealisee`
- `delaiTraitementJoursPrevisionnel` = 30 si `urgence == true` (référé), 90 sinon

## Cas d'erreur

- `domaineDesaccord` invalide ou null → 400
- `intensiteDesaccord` invalide ou null → 400
- `tentativesMediation` contient un code inconnu → 400
- `ageEnfantsConcernes` contient une valeur < 0 ou > 17 → 400
- `dateRequete` null → 400
- Dossier non FRANCE → 400 ("Procédure propre au droit français — F-FA-19 BE non couverte")
- Dossier non DROIT_FAMILLE → 400
- Cross-workspace access → 404

## Critères d'acceptation

1. POST `/api/v1/case-files/{caseFileId}/desaccords-parentaux` domaine `SCOLARITE`, intensité `MAJEUR`, 2 tentatives, intérêt invoqué → score ≥ 70, verdict `ELEVEE`.
2. POST domaine `AUTRE`, intensité `MINEUR`, aucune médiation, sans intérêt → score < 40, verdict `FAIBLE`, médiation recommandée.
3. POST avec un enfant de 14 ans → `auditionEnfantsRecommandee = true`.
4. POST avec aucun enfant ≥ 13 ans → `auditionEnfantsRecommandee = false`.
5. POST avec intensité `MAJEUR` et `expertiseDejaRealisee = false` → `expertisePsyRecommandee = true`.
6. POST avec `expertiseDejaRealisee = true` → `expertisePsyRecommandee = false` même en MAJEUR.
7. POST avec `tentativesMediation = []` → `mediationPrealableRecommandee = true`.
8. POST avec `tentativesMediation = [AUCUNE]` → `mediationPrealableRecommandee = true`.
9. POST avec `urgence = true` → `delaiTraitementJoursPrevisionnel = 30`.
10. POST avec `urgence = false` → `delaiTraitementJoursPrevisionnel = 90`.
11. POST workspace BE → 400.
12. POST workspace DROIT_DU_TRAVAIL → 400.
13. GET après POST → renvoie l'analyse persistée. GET sans POST → 404.
14. Cross-workspace access → 404.
15. Upsert : 2 POST sur le même dossier → un seul enregistrement, dernier résultat retourné.

## Plan de test minimal

- **≥ 14 UT** sur `DesaccordsParentauxCalculator` :
  - score sur les 5 contributions (domaine, intensité, médiation, intérêt, urgence)
  - verdict aux trois seuils (ELEVEE / MOYENNE / FAIBLE)
  - médiation recommandée si vide / AUCUNE seul
  - audition recommandée si au moins un enfant ≥ 13 ans
  - expertise psy recommandée si MAJEUR && !expertiseDejaRealisee
  - délai 30 si urgence, 90 sinon
  - throws sur domaine invalide, intensité invalide, médiation invalide, age invalide
  - score plafonné à 100
- **≥ 8 IT** sur `DesaccordsParentauxControllerIT` :
  - POST nominal FR ELEVEE, POST FAIBLE, GET après POST, GET sans POST → 404, upsert, BE → 400, DT → 400, cross-WS → 404, domaine invalide → 400

## Tables / endpoints / composants impactés

### Endpoints
- POST `/api/v1/case-files/{caseFileId}/desaccords-parentaux`
- GET `/api/v1/case-files/{caseFileId}/desaccords-parentaux`

### Tables
- `desaccords_parentaux_analyses` (nouvelle, migration **145**)
  - `id` UUID PK
  - `case_file_id` UUID FK unique → case_files
  - `domaine_desaccord` varchar(40) NOT NULL
  - `intensite_desaccord` varchar(20) NOT NULL
  - `tentatives_mediation` text NOT NULL (JSON array sérialisé)
  - `age_enfants_concernes` text NOT NULL (JSON array sérialisé)
  - `interet_superieur_invoque` boolean NOT NULL
  - `expertise_deja_realisee` boolean NOT NULL
  - `urgence` boolean NOT NULL
  - `date_requete` date NOT NULL
  - `country` varchar(20) NOT NULL
  - `result_data` text NOT NULL (sérialisation `DesaccordsParentauxResult`)
  - `created_at`, `updated_at` timestamptz NOT NULL

### Visibility rule
- UUID `f1a04001-0000-0000-0000-ee00000fa195`
- Layer ALWAYS_ON, FRANCE + DROIT_FAMILLE
- Tool id `F-FA-19-desaccords-parentaux`
- Priority `79`

### Composants Java
- `DesaccordsParentauxCalculator` (algorithme art. 373-2-10 Cciv)
- `DesaccordsParentauxRequest` / `DesaccordsParentauxResponse` / `DesaccordsParentauxResult` (records)
- `DesaccordsParentauxAnalysis` (entity)
- `DesaccordsParentauxRepository` (Spring Data)
- `DesaccordsParentauxService` (orchestration + persistance + gates)
- `DesaccordsParentauxController` (POST + GET)

## Hors périmètre

- **Frontend** (SF-FA-19-06 livrée en parallèle avec contrat figé)
- Régime BE équivalent (art. 374 §1er Code civil BE — à traiter dans une SF jumelle si retenu après V7)
- Génération de la requête JAF (template / PDF) — voir F-FA-04
- Médiation familiale (organisation pratique) — hors outil décisionnel
- Calcul d'astreinte / sanctions en cas de non-respect (art. 373-2-6)
- Demande conjointe d'autorisation préalable (art. 373-2-1) — feature distincte

## Analyse de cohérence transversale

- **Auth / Principal** : aucune modification — réutilise `OidcUser + Principal + CurrentUserResolver`
- **Workspace context** : aucune modification — gate `workspace.country == "FRANCE"` + `caseFile.legalDomain == "DROIT_FAMILLE"`, aligné sur SF-FA-19-01 et SF-FA-19-03
- **Plans / limites** : aucun gate plan — outil ALWAYS_ON
- **Navigation / routing** : aucune (frontend SF-FA-19-06)
- **Outil décisionnel métier** : nouvel outil. Scan effectué sur les outils décisionnels famille existants (F-FA-08/09/10/11/12/13/14/15/F-FA-19-01/F-FA-19-03) — chacun couvre une situation distincte (divorce altération, divorce faute, divorce accepté, désunion irrémédiable BE, mesures provisoires AOMP, révisions post-divorce, ordonnance de protection, récompenses, autorité parentale exercice, changement de résidence). Les désaccords parentaux art. 373-2-10 sont un **sujet distinct** non couvert. Classement = nouvel outil dédié, pas de scission ni d'extension. Invariant respecté : un outil = une situation métier.

## Nouveau pattern UI ou service partagé

- Aucun composant partagé / DTO réutilisable / endpoint transversal introduit. Contrat strictement scoped à `/api/v1/case-files/{id}/desaccords-parentaux`. Réutilisation des patterns existants (records DTO, entity 1:1 par dossier, service `@Transactional`, calculator pur, visibility rule ALWAYS_ON).

## Impact par domaine métier

- **DROIT_DU_TRAVAIL** : non applicable (sujet famille).
- **DROIT_FAMILLE** : cœur de la SF.
- **IMMIGRATION** : non applicable.
- **FRANCE** : couvert (art. 373-2-10 Cciv + art. 388-1 audition mineurs).
- **BELGIQUE** : non couvert dans cette SF (gate strict 400). Art. 374 §1er Code civil BE applicable — à proposer comme SF jumelle si V7 confirme l'intérêt produit.

## Parité des domaines métier

Cet outil est de **niveau 5** (scoring d'éligibilité). Vérification de l'équivalence sur les 2 autres domaines :
- **DROIT_DU_TRAVAIL** : concept non transposable (désaccords parentaux = sujet famille).
- **IMMIGRATION** : concept non transposable (désaccords parentaux = sujet famille).

L'outil est **intrinsèquement domain-specific** ; aucune feature jumelle requise.

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/desaccords-parentaux`

Request :
```json
{
  "domaineDesaccord": "SCOLARITE",
  "intensiteDesaccord": "MAJEUR",
  "tentativesMediation": ["MEDIATION_FAMILIALE", "DISCUSSIONS_DIRECTES"],
  "ageEnfantsConcernes": [10, 14],
  "interetSuperieurInvoque": true,
  "expertiseDejaRealisee": false,
  "urgence": false,
  "dateRequete": "2026-05-10"
}
```

Enums :
- `domaineDesaccord` : `SCOLARITE`, `SANTE`, `RELIGION`, `LOISIRS_SPORTS`, `CHOIX_EDUCATIFS`, `DEMENAGEMENT`, `AUTRE`
- `intensiteDesaccord` : `MAJEUR`, `MOYEN`, `MINEUR`
- `tentativesMediation` (array) : `MEDIATION_FAMILIALE`, `MEDIATION_JUDICIAIRE`, `DISCUSSIONS_DIRECTES`, `THERAPIE_FAMILIALE`, `AUCUNE`

Response 200 :
```json
{
  "caseFileId": "uuid",
  "domaineDesaccord": "SCOLARITE",
  "intensiteDesaccord": "MAJEUR",
  "tentativesMediation": ["MEDIATION_FAMILIALE", "DISCUSSIONS_DIRECTES"],
  "ageEnfantsConcernes": [10, 14],
  "interetSuperieurInvoque": true,
  "expertiseDejaRealisee": false,
  "urgence": false,
  "dateRequete": "2026-05-10",
  "scoreEligibiliteJaf": 75,
  "verdictProbabiliteAcceptation": "ELEVEE",
  "mediationPrealableRecommandee": false,
  "auditionEnfantsRecommandee": true,
  "expertisePsyRecommandee": true,
  "delaiTraitementJoursPrevisionnel": 90,
  "baseJuridique": "Art. 373-2-10 Cciv + jurisprudence Cass. 1ère civ.",
  "formule": "Domaine SCOLARITE + intensité MAJEUR + 2 tentatives médiation = score 75 (ELEVEE)",
  "messages": ["JAF saisi par requête simple (art. 373-2-10)", "..."],
  "country": "FRANCE"
}
```

Codes erreur :
- `400` : validation (domaine/intensité invalide, médiation invalide, age invalide, dossier BE, dossier DT, payload manquant)
- `404` : dossier inexistant ou cross-workspace
