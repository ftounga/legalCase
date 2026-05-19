# SF-206-05 — Backend : outil décisionnel « prise d'acte de la rupture »

> Feature F-206 — P1 Travail FR — 4 outils d'urgences procédurales.
> Outil : `F-DT-39-prise-acte-rupture`. Fondement : Cass. soc. 25/06/2003 (n° 01-42.679), jurisprudence consolidée.

## Objectif

Fournir le moteur backend qui score, **avant que le salarié ne prenne acte**, les chances que le conseil de prud'hommes retienne les effets d'un licenciement sans cause réelle et sérieuse plutôt que ceux d'une démission.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/prise-acte-rupture` reçoit la saisie de l'avocat, calcule via un `Calculator` stateless, persiste un snapshot, renvoie le verdict. `GET` renvoie le dernier snapshot.

L'analyseur pondère une liste de **griefs imputés à l'employeur** ; le critère jurisprudentiel : les manquements doivent être **suffisamment graves pour empêcher la poursuite du contrat de travail**. Griefs évalués (chacun booléen) :
- défaut / retard de paiement du salaire (aggravé par le montant et la récurrence) ;
- harcèlement moral ou sexuel ;
- manquement à l'obligation de sécurité ;
- modification unilatérale du contrat (rémunération, fonctions, lieu) ;
- déclassement / mise à l'écart ;
- discrimination ;
- non-paiement des heures supplémentaires ;
- non-respect des durées maximales de travail ou des temps de repos.

Modulateurs : `griefsActuelsEtPersistants` (un grief ancien régularisé pèse moins — Cass. soc. 26/03/2014), `montantImpayesEur`, `griefRendImpossiblePoursuite`.

Verdict (`Verdict`) : `PRISE_ACTE_FAVORABLE` (effets licenciement sans cause / nul probables) / `PRISE_ACTE_RISQUEE` / `PRISE_ACTE_DEFAVORABLE` (effets démission probables). Score 0-100 (solidité). Chaque grief retenu = un `GriefRetenu(code, libelle, fondement, poids, explication)`.

## Cas d'erreur

- `caseFileId` inexistant / hors workspace → 404 ; domaine ≠ `DROIT_DU_TRAVAIL` → 422 ; `country` ≠ `FRANCE` → 422.
- `montantImpayesEur` < 0 → 400.

## Contrat API (figé — référence pour SF-206-06)

```
POST /api/v1/case-files/{caseFileId}/prise-acte-rupture
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
  griefsActuelsEtPersistants: boolean,
  griefRendImpossiblePoursuite: boolean,
  griefsCommentaire: String|null
}
Response 200 {
  ...12 champs input (snapshot),
  verdict: PRISE_ACTE_FAVORABLE|PRISE_ACTE_RISQUEE|PRISE_ACTE_DEFAVORABLE,
  scoreSolidite: int,                         // 0-100
  griefsRetenus: [{code, libelle, fondement, poids, explication}],
  effetProbable: enum LICENCIEMENT_SANS_CAUSE|LICENCIEMENT_NUL|DEMISSION,
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/prise-acte-rupture → 200 | 204
```

`critereCode` F-IA-03 : `DT39_DEFAUT_PAIEMENT`, `DT39_HARCELEMENT`, `DT39_MANQUEMENT_SECURITE`, `DT39_MODIFICATION_CONTRAT`, `DT39_GRIEF_IMPOSSIBLE_POURSUITE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `prise_acte_detail` : `priseActeDefautPaiementSalaire`, `priseActeMontantImpayes`, `priseActeHarcelement`, `priseActeManquementSecurite`, `priseActeModificationContrat`, `priseActeDeclassement`, `priseActeDiscrimination`, `priseActeHeuresSupNonPayees`, `priseActeNonRespectRepos`, `priseActeGriefsPersistants`, `priseActeGriefImpossiblePoursuite`.
Extension `LegalDomainPromptBuilder` pour l'extraction de ces griefs.

## Critères d'acceptation

1. Défaut de paiement de salaire + montant significatif + persistant → grief de poids fort, verdict tiré vers `PRISE_ACTE_FAVORABLE`.
2. Harcèlement OU discrimination retenu → `effetProbable=LICENCIEMENT_NUL`.
3. Grief unique, ancien, non persistant (`griefsActuelsEtPersistants=false`) → poids réduit.
4. `griefRendImpossiblePoursuite=false` et griefs mineurs → `PRISE_ACTE_DEFAVORABLE`, `effetProbable=DEMISSION`.
5. Aucun grief coché → `PRISE_ACTE_DEFAVORABLE`, score 0.
6. 422 hors domaine / hors `FRANCE` ; 404 cross-workspace ; 400 entrées invalides.
7. Pré-remplissage : les 11 champs `prise_acte_detail` extraits par le prompt.

## Plan de test

- **UT `PriseActeRuptureCalculatorTest`** : chaque grief isolé ; cumul de griefs ; modulateur persistance ; bascule `LICENCIEMENT_NUL` sur harcèlement/discrimination ; aucun grief ; bornes de score.
- **IT `PriseActeRuptureControllerIT`** : POST + GET, droits, domaine, pays, isolation workspace, validation 400.
- **IT visibilité** : `DecisionToolVisibilityIntegrityIT` vert.

## Tables / endpoints / composants impactés

- **Nouvelle table** `prise_acte_rupture_analyses` — migration Liquibase.
- **Seed** `decision_tool_visibility_rules` : `tool_id=F-DT-39-prise-acte-rupture`, `DROIT_DU_TRAVAIL`, `FRANCE`, `CONTEXTUAL`, `trigger_field=prise_acte_envisagee`, `trigger_value=true`.
- **Nouveaux fichiers** `fr.ailegalcase.casefile` : `PriseActeRuptureCalculator`, `…Request`, `…Response`, `…Input`, `…Result`, `…Analysis`, `…Repository`, `…Service`, `…Controller`.
- **Modifiés** : `CaseAnalysisResponse.java`, `LegalDomainPromptBuilder.java`, `CaseFileDashboardService.java` (mapper `DashboardTile`, thème `DIAGNOSTIC`).

## Préoccupations transversales

**Outil décisionnel métier** — analyseur de scoring. Invariant « un outil = une situation » : situation = évaluation des chances d'une prise d'acte (rupture **immédiate**). **Distinct de SF-206-07** (résiliation judiciaire = saisine en restant en poste) — ne pas fusionner (audit F-191 explicite). Pas d'impact auth/workspace/plan/navigation.

## Hors périmètre

- Frontend (→ SF-206-06).
- Chiffrage des indemnités consécutives (couvert par F-DT-09 comparateur d'indemnités).
- Rédaction de la lettre de prise d'acte (couvert par F-98).
