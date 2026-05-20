# SF-212-39 — Backend : outil décisionnel « exécution forcée du jugement CPH — AGS »

> Feature F-212. Outil : `F-DT-88-execution-jugement-cph`. Fondement : art. 514 CPC (exécution provisoire de droit depuis 2020) ; L. 3253-6 à L. 3253-21 CSS (AGS — Association pour la Gestion du régime de garantie des créances des Salariés) ; L. 625-1+ Code de commerce.

## Objectif

Fournir le moteur backend qui analyse les voies d'exécution d'un jugement prud'homal et détecte l'intervention de l'AGS (garantie des salaires) en cas de procédure collective de l'employeur.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/execution-jugement-cph` + `GET`.

L'analyseur évalue :
- **Exécution provisoire de droit** (art. 514 CPC depuis décret 11/12/2019) : les décisions CPH sont de droit exécutoires à titre provisoire dès leur prononcé — l'appel ne suspend pas l'exécution sauf exception accordée par le Premier Président de la cour d'appel.
- **AGS** (L. 3253-6 CSS) : en cas de redressement ou liquidation judiciaire de l'employeur, l'AGS garantit le paiement des créances salariales impayées dans la limite de plafonds (3 mois de salaire brut — plafonds inchangés depuis 2020 sauf actualisation annuelle).
  - Avances AGS : demande adressée au mandataire judiciaire.
  - Plafond mensuel : environ 5 400 €/mois (valeur 2024 — à vérifier actualisation).
  - Créances couvertes : salaires, préavis, IL, CP, DI licenciement.
- **Saisie sur rémunération** (R. 3252-1) : saisie chez le nouvel employeur si salarié a retrouvé du travail.

Également : flag `execution_jugement_cph_detectee` ajouté à `TravailExtractedData` (nouveau flag non livré par F-205 — extension dans cette SF).

Verdict `AnalyseExecutionJugement` : `EXECUTION_DIRECTE_POSSIBLE` / `AGS_APPLICABLE` / `EXECUTION_COMPLEXE` + montants AGS plafonnés.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-40)

```
POST /api/v1/case-files/{caseFileId}/execution-jugement-cph
Request {
  procedureCollectiveEmployeur: boolean,
  typeProcedurecollective: enum SAUVEGARDE|REDRESSEMENT_JUDICIAIRE|LIQUIDATION_JUDICIAIRE|AUCUNE,
  creancesTotalesEuros: double,
  salaireMensuelBrutEuros: double,
  creancesSalariales3MoisEuros: double,     // salaires, préavis, IL, CP estimés
  jugementPrononce: boolean,
  dateJugement: LocalDate|null
}
Response 200 {
  ...inputs (snapshot),
  analyseExecution: EXECUTION_DIRECTE_POSSIBLE|AGS_APPLICABLE|EXECUTION_COMPLEXE,
  agsApplicable: boolean,
  plafondsAgsMensuelEuros: double,          // plafond mensuel AGS
  montantGarantiEstimeEuros: double,
  executionProvisoire: boolean,             // toujours true si jugement prononcé post-2020
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/execution-jugement-cph → 200 | 204
```

`critereCode` F-IA-03 : `DT88_PROCEDURE_COLLECTIVE`, `DT88_CREANCES_SALARIALES`, `DT88_AGS`, `DT88_EXECUTION_PROVISOIRE`.

**Extension flag IA** (nouveau) : `execution_jugement_cph_detectee` (boolean, niveau 2 sur mention « jugement », « exécuter », « AGS », « liquidation ») ajouté dans `TravailExtractedData` + prompt.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `execution_jugement_detail` + flag `execution_jugement_cph_detectee` :
`executionJugementProcedureCollective`, `executionJugementTypeProcedurecollective`, `executionJugementCreances`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Aucune procédure collective → `EXECUTION_DIRECTE_POSSIBLE`, `executionProvisoire = true` si jugement post-2020.
2. Liquidation judiciaire + créances salariales → `AGS_APPLICABLE`, montant garanti calculé.
3. `plafondsAgsMensuelEuros` cohérent (valeur 2024 documentée).
4. 422 hors `FRANCE`.
5. Isolation workspace → 404.
6. `tool_id=F-DT-88-execution-jugement-cph` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `ExecutionJugementCphCalculatorTest`** : procédure collective vs non ; calcul AGS.
- **IT `ExecutionJugementCphControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `execution_jugement_cph_analyses`.
- **Seed** : `tool_id=F-DT-88-execution-jugement-cph`, `trigger_field=execution_jugement_cph_detectee`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard + extension `TravailExtractedData` (nouveau flag).

## Hors périmètre

Frontend (→ SF-212-40). Saisie-attribution sur compte bancaire (hors périmètre V1). Appel CPH (P2, F-DT-86 → F-218).
