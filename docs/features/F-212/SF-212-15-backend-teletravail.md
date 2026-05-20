# SF-212-15 — Backend : outil décisionnel « télétravail — conformité et litige »

> Feature F-212. Outil : `F-DT-82-teletravail-accord`. Fondement : L. 1222-9 à L. 1222-11 CT ; ANI télétravail 26/11/2020 ; accord d'entreprise ou charte unilatérale.

## Objectif

Fournir le moteur backend qui vérifie la conformité du dispositif de télétravail et analyse les litiges courants (refus de l'employeur, indemnité d'occupation, accident à domicile, retour au bureau imposé).

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/teletravail-accord` + `GET`.

L'analyseur vérifie :
- **Cadre juridique** (L. 1222-9) : accord collectif ou, à défaut, charte unilatérale consultée par le CSE, ou accord individuel.
- **Double volontariat** (L. 1222-9) : sauf circonstances exceptionnelles (ex. pandémie L. 1222-11), le télétravail repose sur le volontariat de l'employeur ET du salarié.
- **Indemnité d'occupation** (ANI 2020 art. 6.2) : remboursement des frais professionnels liés au télétravail (Internet, électricité, matériel) — URSSAF admet 2,60 €/jour ou sur justificatifs.
- **Accident à domicile** (L. 1222-9 al. 4) : présomption d'accident du travail si survenu sur le lieu de télétravail + plage horaire de travail.
- **Retour au bureau** : l'employeur peut imposer le retour si accord/charte le prévoit (délai de prévenance) ou si circonstances justifiées.
- **Refus de télétravailler** : le refus ne peut pas constituer en soi un motif de licenciement (L. 1222-9 al. 6).

Verdict `AnalyseTeletravail` : `CONFORME` / `NON_CONFORME` / `LITIGE_IDENTIFIE` + liste des `PointTeletravail`.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422 (ANI 2020 FR-only).
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-16)

```
POST /api/v1/case-files/{caseFileId}/teletravail-accord
Request {
  cadreTeletravail: enum ACCORD_COLLECTIF|CHARTE_UNILATERALE|ACCORD_INDIVIDUEL|AUCUN,
  doubleVolontariatRespectee: boolean,
  indemnitéOccupationVersee: boolean,
  montantIndemniteJournalierEuros: double|null,
  accidentDomicileDetecte: boolean,
  retourBureauImposeUnilateralement: boolean,
  refusTeletravailCauseIncrimination: boolean
}
Response 200 {
  ...inputs (snapshot),
  analyseTeletravail: CONFORME|NON_CONFORME|LITIGE_IDENTIFIE,
  scoreTeletravail: int,
  pointsTeletravail: [{code, libelle, fondement, conclusion}],
  alerteAccidentTravailDomicile: boolean,
  alerteRefusTeletravailLicenciement: boolean,
  indemniteDueEstimeeEuros: double|null,
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/teletravail-accord → 200 | 204
```

`critereCode` F-IA-03 : `DT82_CADRE_JURIDIQUE`, `DT82_DOUBLE_VOLONTARIAT`, `DT82_INDEMNITE_OCCUPATION`, `DT82_ACCIDENT_DOMICILE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `teletravail_detail` :
`teletravailCadreJuridique`, `teletravailDoubleVolontariat`, `teletravailIndemnitéVersee`, `teletravailAccidentDetecte`, `teletravailRetourBureauImpose`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Aucun cadre juridique → `NON_CONFORME`.
2. Refus télétravail utilisé comme cause licenciement → `alerteRefusTeletravailLicenciement = true`.
3. Accident domicile détecté → `alerteAccidentTravailDomicile = true`.
4. Indemnité d'occupation non versée → point de non-conformité + estimation indemnité due.
5. 422 hors `FRANCE`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-82-teletravail-accord` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `TeletravailAccordCalculatorTest`** : chaque cadre juridique ; alertes ; calcul indemnité.
- **IT `TeletravailAccordControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `teletravail_accord_analyses`.
- **Seed** : `tool_id=F-DT-82-teletravail-accord`, `trigger_field=teletravail_litige_detecte`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-16). Accord sectoriel spécifique hors ANI 2020 (P3, F-218).
