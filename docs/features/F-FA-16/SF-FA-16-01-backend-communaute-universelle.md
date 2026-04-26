# SF-FA-16-01 — Backend communauté universelle (art. 1526 + 1527 Cciv)

## Objectif

Exposer un endpoint POST/GET d'analyse de validité et de liquidation d'un régime matrimonial **conventionnel de communauté universelle** (4ᵉ régime matrimonial français — art. 1526 Cciv), avec gestion de la **clause d'attribution intégrale** (CAI) et du **risque d'action en retranchement** des enfants non communs (art. 1527 al. 2 Cciv).

## Concept métier

La **communauté universelle** est un régime matrimonial **conventionnel** (par contrat de mariage notarié). Tous les biens — présents et à venir, propres ou communs — entrent dans la communauté.

Distinction des 3 régimes déjà couverts dans le projet :
- `COMMUNAUTE_LEGALE` (régime par défaut depuis 1966).
- `SEPARATION_BIENS` (par contrat).
- `PARTICIPATION_ACQUETS` (par contrat).
- **`COMMUNAUTE_UNIVERSELLE`** ← cette SF.

L'outil supporte 2 dispositifs distincts pilotés par `dispositifAnalyse` :

### 1. `VALIDITE_CONVENTION`
Vérification de la validité du contrat de mariage instituant la communauté universelle.
- `contratNotarie` (booléen — obligatoire — sans contrat notarié → NUL).
- `inscriptionEtatCivil` (booléen — opposabilité aux tiers).
- `consentementLibreDesEpoux` (booléen — vice du consentement).
- `respectReserveHereditaire` (booléen — règle 1527 al. 2 si enfants non communs).

### 2. `LIQUIDATION_DECES`
Liquidation suite décès d'un époux.
- Avec **clause d'attribution intégrale** (CAI) → conjoint survivant prend 100 % en franchise de droits de mutation, mais enfants d'un 1er lit peuvent demander **action en retranchement** (art. 1527 al. 2 Cciv).
- Sans CAI → partage classique 50/50 + dévolution successorale sur la moitié du défunt.

Champs liquidation :
- `clauseAttributionIntegrale` (booléen).
- `enfantsNonCommuns` (booléen — déclenche action en retranchement potentielle si CAI).
- `valeurCommunauteEur` (≥ 0).

## Verdict

- `verdictValidite` ∈ {`VALIDE`, `CONTESTABLE`, `NUL`}
  - `NUL` si `contratNotarie=false` (préalable absolu — art. 1394 Cciv).
  - `CONTESTABLE` si `consentementLibreDesEpoux=false` ou (`enfantsNonCommuns=true` et `respectReserveHereditaire=false`).
  - `VALIDE` sinon.

## Sortie

`verdictValidite`, `actionRetranchementPossible` (boolean — true si LIQUIDATION_DECES + CAI + enfantsNonCommuns), `partAttributionConjointPct` (typiquement 100 % si CAI, 50 % sinon ; 0 si NUL), `valeurAttributionEur` (% × valeurCommunauteEur), `risquesIdentifies` (List<String>), `baseJuridique` (Art. 1526 Cciv + 1527 al. 2 Cciv), `formule`, `messages`, `country`, `scoreValidite` (0-100).

## Comportement nominal

- POST `/api/v1/case-files/{caseFileId}/communaute-universelle-analysis` : calcule + persiste (upsert 1:1 par dossier).
- GET `/api/v1/case-files/{caseFileId}/communaute-universelle-analysis` : renvoie la dernière analyse (404 si aucune).

## Cas d'erreur

- 400 si `dispositifAnalyse` manquant.
- 400 si `contratNotarie` null (champ commun aux 2 dispositifs).
- 400 si dispositif `LIQUIDATION_DECES` sans `clauseAttributionIntegrale`, `enfantsNonCommuns` ou `valeurCommunauteEur`.
- 400 si dispositif `VALIDITE_CONVENTION` sans `inscriptionEtatCivil`, `consentementLibreDesEpoux`, `respectReserveHereditaire`.
- 400 si `valeurCommunauteEur < 0`.
- 400 si workspace BELGIQUE (single-country FR — l'équivalent BE relève d'un régime différent).
- 400 si dossier non `DROIT_FAMILLE`.
- 404 si dossier hors workspace utilisateur.
- 404 si GET sans POST préalable.

## Critères d'acceptation vérifiables

1. POST FR + `VALIDITE_CONVENTION` + `contratNotarie=true` + tous critères → `verdictValidite=VALIDE`.
2. POST FR + `VALIDITE_CONVENTION` + `contratNotarie=false` → `verdictValidite=NUL`, `partAttributionConjointPct=0`.
3. POST FR + `VALIDITE_CONVENTION` + `consentementLibreDesEpoux=false` → `verdictValidite=CONTESTABLE`.
4. POST FR + `LIQUIDATION_DECES` + CAI + enfants non communs → `actionRetranchementPossible=true`, `partAttributionConjointPct=100`, message contient « retranchement ».
5. POST FR + `LIQUIDATION_DECES` + CAI + enfants tous communs → `actionRetranchementPossible=false`, `partAttributionConjointPct=100`.
6. POST FR + `LIQUIDATION_DECES` sans CAI → `partAttributionConjointPct=50`, `actionRetranchementPossible=false`.
7. `valeurAttributionEur = pct × valeurCommunauteEur`.
8. `baseJuridique` contient « 1526 » et « 1527 ».
9. POST workspace BE → 400.
10. POST workspace immigration FR → 400.
11. Cross-workspace → 404.
12. Upsert : second POST remplace l'analyse précédente.
13. GET sans POST → 404.

## Plan de test

- **Unit** (`CommunauteUniverselleCalculatorTest`, ≥ 15) : verdicts (VALIDE/CONTESTABLE/NUL), liquidation avec/sans CAI, action retranchement, valeur attribution, base juridique, validations (nulls, négatifs, country BE), country FRANCE normalisé, dispositif null.
- **IT** (`CommunauteUniverselleControllerIT`, ≥ 7) : POST validité VALIDE, POST validité NUL (contrat non notarié), POST liquidation avec CAI + enfants non communs, POST liquidation sans CAI, POST workspace BE → 400, POST DROIT_DU_TRAVAIL → 400, POST cross-workspace → 404, upsert, GET 404 sans POST.

## Tables / endpoints / composants impactés

- **Migration Liquibase** : `177-create-communaute-universelle-analyses.xml` — table `communaute_universelle_analyses` (UNIQUE `case_file_id`) + INSERT `decision_tool_visibility_rules` (ALWAYS_ON, DROIT_FAMILLE, FRANCE, priority **86**, UUID `f1a04001-0000-0000-0000-ee0000000177`, tool_id `'F-FA-16-communaute-universelle'`).
- **Backend** : `CommunauteUniverselleRequest/Response/Result/Analysis/Repository/Calculator/Service/Controller`.

## Note référentiel

L'enum `regime_matrimonial` est géré côté Java (codes utilisés dans `RecompensesCalculator`, etc.). Aucune entry `legal_referentials` à ajouter pour cette SF — l'outil utilise un enum interne `DispositifAnalyse` propre au calculator. Pas de divergence Java/DB à risquer.

## Hors périmètre

- Frontend (SF-FA-16-02 séparée).
- Belgique — la communauté universelle BE a un régime différent (Code civil belge livre III) → backlog jumeau si pertinent.
- Calcul détaillé du retranchement (montant exact à reverser aux enfants non communs) → simulateur dédié au backlog.
- Donations entre époux liées (clause de préciput / clause alsacienne) → SF future.

## Impact par domaine métier

Feature **sensible au domaine** :
- **Droit du travail / Immigration** : non applicable (gate domain → 400).
- **Droit famille** :
  - **France** : couvert par cette SF (art. 1526 + 1527 Cciv).
  - **Belgique** : régime distinct (Code civil belge) — **non couvert par cette SF**. Ouverture potentielle d'une feature jumelle au backlog si demande métier.

## Parité des domaines métier (outil de niveau 5 — scoring de validité)

- **Droit du travail FR/BE** : non applicable (sphère matrimoniale).
- **Droit immigration FR/BE** : non applicable.
- **Droit famille FR** : couvert par cette SF.
- **Droit famille BE** : régime juridique distinct — à ouvrir au backlog comme feature jumelle si pertinent (Code civil belge).

## Analyse de cohérence transversale

- **Outils décisionnels existants droit famille FR** : F-FA-15 (Récompenses art. 1437) couvre les régimes communautaires y compris `COMMUNAUTE_UNIVERSELLE` côté calcul de récompenses. Cette SF F-FA-16 traite la **validité de la convention** + la **liquidation au décès**, pas la mécanique de récompense. Pas de doublon : F-FA-15 calcule les récompenses entre époux, F-FA-16 valide le contrat et liquide.
- **Pattern de référence** : strict alignement avec `PartageJudiciaireCalculator` (PR #636 — pattern famille FR récent) — types enum, normalisation country, gate FRANCE, builder messages, structure result/response/analysis.
- **Patterns transversaux** : aucun nouveau composant partagé / DTO / directive / service. Réutilisation stricte de `CurrentUserResolver`, `OAuthProviderResolver`, `WorkspaceMemberRepository`, `ObjectMapper` existants.

## Préoccupations transversales

- **Auth / Principal** : aucun changement (réutilise pattern existant `CurrentUserResolver` + `OAuth2AuthenticationToken`).
- **Workspace context** : aucun changement (gate via `caseFile.getWorkspace().getCountry()`).
- **Plans / limites** : non concerné.
- **Navigation / routing** : aucun ajout (endpoints REST seulement).
- **Outil décisionnel métier** : nouveau outil. Scan effectué — un outil = une situation métier (validité + liquidation au décès dans un seul outil car les 2 dispositifs partagent la même base de critères du contrat de mariage et sont mutuellement contextuels — choix conforme au pattern F-FA-15 Récompenses qui regroupe 3 régimes en un seul calculator). Pas de switch country dans le calculator (gate dur en service).

## Contrat API (figé pour parallélisation éventuelle)

### POST `/api/v1/case-files/{caseFileId}/communaute-universelle-analysis`

Body — dispositif `VALIDITE_CONVENTION` :
```json
{
  "dispositifAnalyse": "VALIDITE_CONVENTION",
  "contratNotarie": true,
  "inscriptionEtatCivil": true,
  "consentementLibreDesEpoux": true,
  "respectReserveHereditaire": true,
  "clauseAttributionIntegrale": null,
  "enfantsNonCommuns": null,
  "valeurCommunauteEur": null
}
```

Body — dispositif `LIQUIDATION_DECES` :
```json
{
  "dispositifAnalyse": "LIQUIDATION_DECES",
  "contratNotarie": true,
  "inscriptionEtatCivil": null,
  "consentementLibreDesEpoux": null,
  "respectReserveHereditaire": null,
  "clauseAttributionIntegrale": true,
  "enfantsNonCommuns": true,
  "valeurCommunauteEur": 800000.0
}
```

Response 200 :
```json
{
  "caseFileId": "uuid",
  "dispositifAnalyse": "VALIDITE_CONVENTION" | "LIQUIDATION_DECES",
  "verdictValidite": "VALIDE" | "CONTESTABLE" | "NUL",
  "actionRetranchementPossible": true,
  "partAttributionConjointPct": 100,
  "valeurAttributionEur": 800000.0,
  "scoreValidite": 90,
  "risquesIdentifies": ["..."],
  "baseJuridique": "Art. 1526 Cciv + 1527 al. 2 Cciv (action en retranchement)",
  "formule": "...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

Codes d'erreur : 400 (validation, BE, mauvais domaine), 404 (case file inconnu / autre workspace, GET sans POST).
