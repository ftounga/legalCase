# SF-206-07 — Backend : outil décisionnel « résiliation judiciaire du contrat »

> Feature F-206 — P1 Travail FR — 4 outils d'urgences procédurales.
> Outil : `F-DT-40-resiliation-judiciaire-cph`. Fondement : Cass. soc. 16/03/1989, Cass. soc. 20/01/1998, art. L. 1411-1 CT, art. 1224/1227-1228 C. civ.

## Objectif

Fournir le moteur backend qui évalue l'**opportunité d'une demande de résiliation judiciaire** du contrat de travail aux torts de l'employeur — le salarié restant en poste pendant l'instance — et la situe en regard de la prise d'acte.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/resiliation-judiciaire-cph` reçoit la saisie de l'avocat, calcule via un `Calculator` stateless, persiste un snapshot, renvoie le verdict. `GET` renvoie le dernier snapshot.

L'analyseur pondère les **manquements de l'employeur** (même socle de griefs que la prise d'acte) avec les spécificités de la résiliation judiciaire :
- le juge apprécie les manquements **au jour de sa décision** — un grief régularisé ou non persistant pèse moins ;
- la résiliation prononcée produit les effets d'un licenciement sans cause réelle et sérieuse, à la date de la décision (ou à la date du licenciement si le salarié a été licencié en cours d'instance) ;
- voie **moins risquée** que la prise d'acte : en cas de rejet, le contrat se poursuit (à signaler dans les `messages`).

Griefs évalués (booléens) : défaut / retard de paiement du salaire, harcèlement, manquement à l'obligation de sécurité, modification unilatérale du contrat, déclassement, discrimination, heures supplémentaires non payées, non-respect des durées de travail.
Modulateurs : `manquementsPersistantsAuJourDemande`, `salarieToujoursEnPoste`, `licenciementIntervenuEnCoursInstance`, `montantImpayesEur`.

Verdict (`Verdict`) : `RESILIATION_FAVORABLE` / `RESILIATION_INCERTAINE` / `RESILIATION_DEFAVORABLE`. Score 0-100. Chaque manquement retenu = un `ManquementRetenu(code, libelle, fondement, poids, explication)`.

## Cas d'erreur

- `caseFileId` inexistant / hors workspace → 404 ; domaine ≠ `DROIT_DU_TRAVAIL` → 422 ; `country` ≠ `FRANCE` → 422.
- `montantImpayesEur` < 0 → 400.

## Contrat API (figé — référence pour SF-206-08)

```
POST /api/v1/case-files/{caseFileId}/resiliation-judiciaire-cph
Request {
  defautPaiementSalaire: boolean,
  montantImpayesEur: BigDecimal|null,
  harcelement: boolean,
  manquementSecurite: boolean,
  modificationUnilateraleContrat: boolean,
  declassement: boolean,
  discrimination: boolean,
  heuresSupNonPayees: boolean,
  nonRespectDureesRepos: boolean,
  manquementsPersistantsAuJourDemande: boolean,
  salarieToujoursEnPoste: boolean,
  licenciementIntervenuEnCoursInstance: boolean,
  manquementsCommentaire: String|null
}
Response 200 {
  ...13 champs input (snapshot),
  verdict: RESILIATION_FAVORABLE|RESILIATION_INCERTAINE|RESILIATION_DEFAVORABLE,
  scoreSolidite: int,                         // 0-100
  manquementsRetenus: [{code, libelle, fondement, poids, explication}],
  dateEffetProbable: enum DATE_DECISION|DATE_LICENCIEMENT,
  basesJuridiques: [String],
  messages: [String],                         // dont rappel « voie sans risque de rupture »
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/resiliation-judiciaire-cph → 200 | 204
```

`critereCode` F-IA-03 : `DT40_DEFAUT_PAIEMENT`, `DT40_HARCELEMENT`, `DT40_MANQUEMENT_SECURITE`, `DT40_MODIFICATION_CONTRAT`, `DT40_MANQUEMENTS_PERSISTANTS`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `resiliation_judiciaire_detail` : `resiliationJudDefautPaiementSalaire`, `resiliationJudMontantImpayes`, `resiliationJudHarcelement`, `resiliationJudManquementSecurite`, `resiliationJudModificationContrat`, `resiliationJudDeclassement`, `resiliationJudDiscrimination`, `resiliationJudHeuresSupNonPayees`, `resiliationJudNonRespectRepos`, `resiliationJudManquementsPersistants`, `resiliationJudSalarieEnPoste`, `resiliationJudLicenciementEnCours`.
Extension `LegalDomainPromptBuilder` pour l'extraction.

## Critères d'acceptation

1. Manquements graves + persistants au jour de la demande → `RESILIATION_FAVORABLE`.
2. `manquementsPersistantsAuJourDemande=false` → poids des griefs réduit (le juge statue au jour de sa décision).
3. `licenciementIntervenuEnCoursInstance=true` → `dateEffetProbable=DATE_LICENCIEMENT` ; sinon `DATE_DECISION`.
4. Aucun manquement → `RESILIATION_DEFAVORABLE`, score 0.
5. Les `messages` rappellent que le rejet de la demande ne rompt pas le contrat (voie moins risquée que la prise d'acte).
6. 422 hors domaine / hors `FRANCE` ; 404 cross-workspace ; 400 entrées invalides.
7. Pré-remplissage : les 12 champs `resiliation_judiciaire_detail` extraits par le prompt.

## Plan de test

- **UT `ResiliationJudiciaireCphCalculatorTest`** : chaque manquement ; modulateur persistance ; bascule `dateEffetProbable` ; aucun manquement ; bornes de score.
- **IT `ResiliationJudiciaireCphControllerIT`** : POST + GET, droits, domaine, pays, isolation workspace, validation 400.
- **IT visibilité** : `DecisionToolVisibilityIntegrityIT` vert.

## Tables / endpoints / composants impactés

- **Nouvelle table** `resiliation_judiciaire_cph_analyses` — migration Liquibase.
- **Seed** `decision_tool_visibility_rules` : `tool_id=F-DT-40-resiliation-judiciaire-cph`, `DROIT_DU_TRAVAIL`, `FRANCE`, `CONTEXTUAL`, `trigger_field=resiliation_judiciaire_envisagee`, `trigger_value=true`.
- **Nouveaux fichiers** `fr.ailegalcase.casefile` : `ResiliationJudiciaireCphCalculator`, `…Request`, `…Response`, `…Input`, `…Result`, `…Analysis`, `…Repository`, `…Service`, `…Controller`.
- **Modifiés** : `CaseAnalysisResponse.java`, `LegalDomainPromptBuilder.java`, `CaseFileDashboardService.java` (mapper `DashboardTile`, thème `DIAGNOSTIC`).

## Préoccupations transversales

**Outil décisionnel métier** — analyseur de scoring. Invariant « un outil = une situation » : situation = opportunité d'une résiliation judiciaire (saisine du CPH **en restant en poste**). **Distinct de SF-206-05** (prise d'acte = rupture immédiate) — outils jumeaux mais non fusionnés (audit F-191 explicite). Pas d'impact auth/workspace/plan/navigation.

## Hors périmètre

- Frontend (→ SF-206-08).
- Génération des conclusions de résiliation judiciaire (couvert par F-98).
- Chiffrage des indemnités consécutives (couvert par F-DT-09).
