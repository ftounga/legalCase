# SF-FA-17-01 — Backend partage judiciaire (art. 1364 CPC + 840+ Cciv)

## Objectif

Exposer un endpoint POST/GET d'analyse de recevabilité d'une procédure de **partage judiciaire** (succession ou post-divorce) côté France, en s'appuyant sur les critères des art. 840 et s. Code civil et 1364 et s. Code de procédure civile.

## Concept métier

Procédure quand les co-indivisaires n'arrivent pas à s'accorder sur la liquidation. 3 étapes possibles :

1. **PROCES_VERBAL_DIFFICULTES** — le notaire dresse un PV constatant le désaccord (art. 1366 CPC) — préalable obligatoire.
2. **HOMOLOGATION_AMIABLE_PARTIELLE** — accord partiel : juge homologue points convenus + tranche litigieux.
3. **PARTAGE_JUDICIAIRE_INTEGRAL** — désaccord total : tribunal procède au partage avec expertise notariale (art. 1364 CPC) + tirage au sort des lots ou licitation si bien indivisible.

## Critères d'éligibilité ELEVEE

- `pvDifficultesEtabli = true` (préalable obligatoire art. 1366)
- `tentativeAmiableEpuiseuee = true` (échec voie amiable — sinon refus pour défaut d'intérêt à agir)
- `typeBienIndivision ∈ {IMMEUBLE_DIVISIBLE, IMMEUBLE_INDIVISIBLE, MEUBLES_DIVERS, MIXTE}`
- `nombreCoindivisaires ≥ 2`
- `desaccordMotive = true` (motif documenté du désaccord)

## Verdict

- **ELEVEE** si tous les critères + bien divisible
- **MOYENNE** si bien indivisible (procédure plus longue + licitation potentielle)
- **FAIBLE** si PV non établi ou tentative amiable non documentée

## Sortie

`verdictRecevabilite`, `dureeProcedureMois` (6-18), `fraisEstimesEur` (provision notariale 2-5% biens), `risqueLicitation` (boolean si bien indivisible), `baseJuridique` (art. 840+ Cciv + 1364+ CPC), `formule`, `messages`.

## Comportement nominal

- POST `/api/v1/case-files/{caseFileId}/partage-judiciaire-analysis` : calcule + persiste (upsert 1:1).
- GET `/api/v1/case-files/{caseFileId}/partage-judiciaire-analysis` : renvoie la dernière analyse (404 si aucune).

## Cas d'erreur

- 400 si critères obligatoires manquants (typeBienIndivision, nombreCoindivisaires<2, valeurEstimeeBiensEur<0).
- 400 si workspace BELGIQUE (single-country FR — équivalent BE différent au backlog).
- 400 si dossier non DROIT_FAMILLE.
- 404 si dossier hors workspace de l'utilisateur.
- 404 si GET sans POST préalable.

## Critères d'acceptation vérifiables

1. POST FR + tous critères + bien divisible → verdict `ELEVEE`, `risqueLicitation=false`, `dureeProcedureMois ∈ [6;12]`.
2. POST FR + bien indivisible → verdict `MOYENNE`, `risqueLicitation=true`, `dureeProcedureMois ∈ [12;18]`.
3. POST FR + PV non établi → verdict `FAIBLE`.
4. POST FR + tentative amiable non documentée → verdict `FAIBLE`.
5. `fraisEstimesEur` calculés à 2-5% de la valeur biens (borne basse si bien divisible, haute sinon).
6. `baseJuridique` contient « 840 », « 1364 » et « 1366 ».
7. POST workspace BE → 400.
8. POST workspace immigration FR → 400.
9. Cross-workspace → 404.
10. Upsert : second POST remplace l'analyse précédente.
11. GET sans POST → 404.

## Plan de test

- **Unit** (`PartageJudiciaireCalculatorTest`, ≥18) : verdicts (ELEVEE/MOYENNE/FAIBLE), types de biens, frais estimés, durées, validations (nulls, négatifs, country BE), bien indivisible → MOYENNE, PV non établi → FAIBLE.
- **IT** (`PartageJudiciaireControllerIT`, ≥7) : POST nominal ELEVEE, POST bien indivisible MOYENNE, POST workspace BE → 400, POST workspace immigration → 400, POST cross-workspace → 404, upsert, GET 404 sans POST.

## Tables / endpoints / composants impactés

- **Migration Liquibase** : `169-create-partage-judiciaire-analyses.xml` — table `partage_judiciaire_analyses` (UNIQUE `case_file_id`) + INSERT `decision_tool_visibility_rules` (ALWAYS_ON, DROIT_FAMILLE, FRANCE, priority **85**, UUID `f1a04001-0000-0000-0000-ee0000000169`, tool_id `'F-FA-17-partage-judiciaire'`).
- **Backend** : `PartageJudiciaireRequest/Response/Result/Analysis/Repository/Calculator/Service/Controller`.

## Hors périmètre

- Frontend (SF-FA-17-02 séparée).
- Belgique — partage judiciaire BE diffère substantiellement (juge de paix, art. 1207 CJ + ss.) → backlog jumeau.
- Génération automatique du PV de difficultés → SF future (lien outil F-DT-XX).
- Calcul détaillé des lots → simulateur dédié (backlog).

## Impact par domaine métier

Feature **sensible au domaine** :
- **Droit du travail / Immigration** : non applicable (404 / 400 par gate).
- **Droit famille** :
  - **France** : couvert par cette SF (art. 840+ Cciv + 1364+ CPC).
  - **Belgique** : régime différent (CJ art. 1207 et s., juge de paix) — **feature jumelle au backlog** (à ouvrir).

## Parité des domaines métier (outil de niveau 5 — scoring de validité)

- **Droit du travail FR/BE** : le partage judiciaire n'a pas d'équivalent direct (sphère matrimoniale/successorale) — non applicable.
- **Droit immigration FR/BE** : non applicable.
- **Droit famille FR** : couvert par cette SF.
- **Droit famille BE** : équivalent existant en CJ art. 1207 et s. — **à ouvrir au backlog comme feature jumelle F-FA-17-BE**. Justification de l'asymétrie temporaire : régime juridique distinct (juge de paix BE vs TGI FR, modalités d'expertise différentes).

## Analyse de cohérence transversale

- **Outils décisionnels existants** : F-FA-17 vient compléter le bloc partage/liquidation déjà couvert par F-FA-04 (Indivision), F-FA-05 (Partage immobilier), F-FA-21 (Séparation de corps). Pas de doublon — partage judiciaire est l'étape contentieuse quand l'amiable échoue. Outil **single-country FRANCE** = pattern uniforme avec F-DT-30 (Protection RP), F-FA-21 (Séparation de corps). Pas de switch country dans le calculator (gate dur en service).
- **Patterns transversaux** : aucun nouveau composant partagé / DTO / directive / service. Réutilisation stricte du pattern `ProtectionRp*` (PR #631 mergé), de `CurrentUserResolver`, `OAuthProviderResolver`, `WorkspaceMemberRepository` existants.

## Préoccupations transversales

- **Auth / Principal** : aucun changement (réutilise pattern existant).
- **Workspace context** : aucun changement.
- **Plans / limites** : non concerné.
- **Navigation / routing** : aucun ajout (endpoints REST seulement, pas de route Angular).
- **Outil décisionnel métier** : nouveau outil, scan effectué — un outil = une situation métier (partage judiciaire ≠ partage amiable F-FA-05 ≠ indivision F-FA-04). Pas de mélange de situations dans ce calculator.

## Contrat API (figé pour parallélisation éventuelle)

### POST `/api/v1/case-files/{caseFileId}/partage-judiciaire-analysis`

Body :
```json
{
  "etapeProcedure": "PROCES_VERBAL_DIFFICULTES" | "HOMOLOGATION_AMIABLE_PARTIELLE" | "PARTAGE_JUDICIAIRE_INTEGRAL",
  "typeBienIndivision": "IMMEUBLE_DIVISIBLE" | "IMMEUBLE_INDIVISIBLE" | "MEUBLES_DIVERS" | "MIXTE",
  "nombreCoindivisaires": 2,
  "valeurEstimeeBiensEur": 250000.0,
  "pvDifficultesEtabli": true,
  "tentativeAmiableEpuiseuee": true,
  "desaccordMotive": true
}
```

Response 200 :
```json
{
  "caseFileId": "uuid",
  "verdictRecevabilite": "ELEVEE" | "MOYENNE" | "FAIBLE",
  "dureeProcedureMois": 12,
  "fraisEstimesEur": 7500.0,
  "risqueLicitation": false,
  "scoreEligibilite": 90,
  "baseJuridique": "Art. 840 et s. Cciv + 1364 et s. + 1366 CPC",
  "formule": "...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

Codes d'erreur : 400 (validation), 404 (case file inconnu / autre workspace).
