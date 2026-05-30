# Mini-spec — F-218 / SF-218-03 — Exécution du jugement CPH (AGS) — backend

## Identifiant

`F-218 / SF-218-03`

## Feature parente

`F-218a` — Procédure CPH avancée (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-03-execution-jugement-cph-backend`

---

## Objectif

Produire la checklist d'exécution forcée d'un jugement CPH (exécution provisoire de droit ex art. R. 1454-28 / art. 514 CPC, signification, commandement) et détecter l'éligibilité à la garantie AGS lorsque l'employeur est en redressement ou liquidation judiciaire, car aucun outil n'accompagne la phase post-jugement.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/execution-jugement-cph-analysis`
- Body :
  - `dateJugement` (LocalDate, requis)
  - `montantCondamnation` (Double > 0, requis) — montant total des condamnations en faveur du salarié
  - `executionProvisoireOrdonnee` (boolean, défaut true) — exécution provisoire de droit pour les créances salariales L. 1454-28 (max 9 mois moyenne salaire)
  - `situationEmployeur` (enum `IN_BONIS` | `REDRESSEMENT` | `LIQUIDATION`, requis)
  - `dateOuvertureProcedureCollective` (LocalDate, optionnel — requis si REDRESSEMENT/LIQUIDATION)
  - `creancesSuperPrivilegiees` (Double ≥ 0, optionnel) — montant des 60 derniers jours de salaire (super-privilège)
- Analyzer `ExecutionJugementCphAnalyzer` :
  - **Checklist exécution** : signification du jugement (préalable obligatoire), exécution provisoire de droit pour les créances salariales (art. R. 1454-28 ; art. 514 CPC), commandement de payer, mandatement huissier, mesures conservatoires si IN_BONIS. Chaque item = `{ libelle, obligatoire, baseJuridique }`.
  - **Détecteur AGS** : si `situationEmployeur` ∈ {REDRESSEMENT, LIQUIDATION} → `agsEligible = true`. Calcule les plafonds AGS (L. 3253-6 et s. ; plafond 6, 5 ou 4 selon ancienneté du contrat — **constante AGS_PLAFOND_MENSUEL à actualiser annuellement**, valeur 2026 documentée), détermine `relaisAgsRecommande = true`, oriente vers la déclaration de créance au mandataire et la saisine CGEA. Item bloquant si `dateOuvertureProcedureCollective` absente.
  - **Verdict** : `EXECUTION_DIRECTE` (IN_BONIS), `RELAIS_AGS` (REDRESSEMENT/LIQUIDATION), `BLOQUE_INFO_MANQUANTE` (procédure collective sans date).
  - `baseJuridique` : art. 514 CPC ; R. 1454-28 CPC ; L. 3253-6 à L. 3253-21 Code travail.
- Output persisté dans `execution_jugement_cph_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/execution-jugement-cph-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| dateJugement absente / future | 400 |
| montantCondamnation ≤ 0 | 400 |
| situationEmployeur inconnue | 400 |
| situationEmployeur=REDRESSEMENT/LIQUIDATION sans dateOuvertureProcedureCollective | 400 |
| caseFile inaccessible | 404 |

---

## Source juridique

- **Art. 514 CPC** — exécution provisoire de droit des décisions de première instance.
- **R. 1454-28 CPC** — exécution provisoire de droit prud'homale (créances salariales, limite 9 mois moyenne salaire).
- **L. 3253-6 à L. 3253-21 Code travail** — garantie AGS : créances couvertes, plafonds, intervention CGEA.
- **L. 3253-8** — créances garanties par l'AGS (super-privilège des 60 derniers jours).
- **Plafond AGS** : 6 / 5 / 4 fois le plafond mensuel SS selon l'ancienneté du contrat — **constante à actualiser annuellement** (`AGS_PLAFOND_MENSUEL_SS` valeur 2026).

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `montantCondamnation` | nombre | dérivé synthèse (nouveau `montantCondamnationCph`) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `situationEmployeur` | enum | `situationEmployeurDetectee` (nouveau, proxy redressement/liquidation) | [x] record + [x] prompt |

**Flag CONTEXTUAL pivot** : `execution_jugement_cph_envisagee` (niveau 3, FR-only, default false) — nouveau flag. Bascule CONTEXTUAL quand l'IA détecte un jugement CPH favorable + difficulté d'exécution (mention « exécution », « huissier », « redressement / liquidation employeur », « AGS / CGEA »).

---

## Critères d'acceptation

- [ ] POST `situationEmployeur=IN_BONIS` → verdict `EXECUTION_DIRECTE`, `agsEligible=false`
- [ ] POST `situationEmployeur=LIQUIDATION` + date ouverture → verdict `RELAIS_AGS`, `agsEligible=true`, plafonds AGS calculés
- [ ] POST `situationEmployeur=REDRESSEMENT` sans `dateOuvertureProcedureCollective` → 400
- [ ] POST `montantCondamnation=0` → 400
- [ ] POST `dateJugement` future → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_FAMILLE → 400
- [ ] GET sans POST → 404 ; upsert sur double POST
- [ ] Isolation workspace
- [ ] Constante `AGS_PLAFOND_MENSUEL_SS` documentée « à actualiser annuellement »
- [ ] Seed `decision_tool_visibility_rules` : CONTEXTUAL, trigger_field=`execution_jugement_cph_envisagee`, trigger_value=`true`
- [ ] `F-DT-88-execution-jugement-cph` dans `KNOWN_FRONTEND_TOOL_IDS`

## Plan de test minimal

- **UT** `ExecutionJugementCphAnalyzerTest` : ≥ 6 cas (in bonis, redressement, liquidation, plafond AGS, info manquante, checklist signification)
- **IT** `ExecutionJugementCphControllerIT` : ≥ 5 cas (200 nominal, 400 country, 400 procédure collective sans date, 404 isolation, upsert)

## Tables / endpoints / composants impactés

- **Nouvelle table** `execution_jugement_cph_analyses`
- **Migration Liquibase** + seed visibility rules
- **Endpoint** `ExecutionJugementCphController`
- **Service** `ExecutionJugementCphService` + **Analyzer** `ExecutionJugementCphAnalyzer`
- **Extension** `TravailExtractedData` : `montantCondamnationCph`, `situationEmployeurDetectee`, flag `executionJugementCphEnvisagee` + prompt
- **Constante** `AgsBareme.AGS_PLAFOND_MENSUEL_SS` (à actualiser annuellement)
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-04)
- Génération de la déclaration de créance au mandataire (générateur futur)
- Calcul détaillé du super-privilège par tranche (forfait V1 via `creancesSuperPrivilegiees` saisi)
