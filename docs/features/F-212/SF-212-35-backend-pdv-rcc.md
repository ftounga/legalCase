# SF-212-35 — Backend : outil décisionnel « PDV / RCC — conformité »

> Feature F-212. Outil : `F-DT-46-pdv-rcc`. Fondement : L. 1237-17 à L. 1237-19-14 CT ; décret 2017-1718 du 20/12/2017 ; circulaire DGT du 19/09/2018.

## Objectif

Fournir le moteur backend qui analyse la conformité d'une rupture conventionnelle collective (RCC) ou d'un plan de départs volontaires (PDV), dispositif distinct du PSE (F-DT-14) et de la rupture conventionnelle individuelle (F-DT-10).

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/pdv-rcc-conformite` + `GET`.

La **RCC** (L. 1237-19) est instituée par un accord collectif majoritaire : elle ouvre droit aux allocations chômage pour les salariés volontaires (contrairement au PDV classique). Procédure : accord collectif → dépôt DREETS → validation de la DREETS (15 j) → adhésion individuelle des salariés.

Le **PDV** (plan de départs volontaires) est possible dans le cadre d'un PSE mais aussi unilatéralement si aucun licenciement contraint n'est prévu. Les départs sont volontaires.

L'analyseur vérifie :
- **Nature du dispositif** : RCC (accord collectif + validation DREETS) ou PDV (accord ou décision unilatérale).
- **Accord collectif RCC** : signé par syndicats représentant ≥ 50 % votes au premier tour (majorité renforcée L. 1237-19-1).
- **Validation DREETS** : dans les 15 j suivant dépôt accord.
- **Indemnités de départ** : au moins égales à l'indemnité légale de licenciement (L. 1237-19-1 al. 5).
- **Adhésion individuelle** : délai de réflexion, information individuelle, libre consentement.

Verdict `AnalysePdvRcc` : `CONFORME` / `PARTIELLEMENT_CONFORME` / `NON_CONFORME` + points d'irrégularité.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422 (RCC FR-only, ord. 22/09/2017).
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-36)

```
POST /api/v1/case-files/{caseFileId}/pdv-rcc-conformite
Request {
  typeDispositif: enum RCC|PDV,
  accordCollectifMajoritaire: boolean,
  validationDREETSObtenue: boolean,
  delaiValidation15JRespectee: boolean|null,
  indemnitesAuMoinsLegales: boolean,
  adhesionVolontaireIndividuelle: boolean,
  informationIndividuelleComplete: boolean
}
Response 200 {
  ...inputs (snapshot),
  analysePdvRcc: CONFORME|PARTIELLEMENT_CONFORME|NON_CONFORME,
  scoreConformite: int,
  pointsIrregularite: [{code, libelle, fondement}],
  droitAreConfirme: boolean,               // true si RCC conforme → droit ARE
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/pdv-rcc-conformite → 200 | 204
```

`critereCode` F-IA-03 : `DT46_ACCORD_MAJORITAIRE`, `DT46_VALIDATION_DREETS`, `DT46_INDEMNITEES`, `DT46_ADHESION_VOLONTAIRE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `pdv_rcc_detail` :
`pdvRccTypeDispositif`, `pdvRccAccordMajoritaire`, `pdvRccValidationDREETS`, `pdvRccIndemnitesLegales`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. RCC + accord majoritaire + validation DREETS + indemnités ≥ légales → `CONFORME`, `droitAreConfirme = true`.
2. Accord non majoritaire → `NON_CONFORME`.
3. Validation DREETS > 15 j → point d'irrégularité.
4. 422 hors `FRANCE`.
5. Isolation workspace → 404.
6. `tool_id=F-DT-46-pdv-rcc-conformite` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `PdvRccConformiteCalculatorTest`** : RCC conforme ; accord non majoritaire ; DREETS manquant.
- **IT `PdvRccConformiteControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `pdv_rcc_conformite_analyses`.
- **Seed** : `tool_id=F-DT-46-pdv-rcc-conformite`, `trigger_field=pdv_rcc_envisage`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-36). PSE (couvert F-DT-14). Rupture conventionnelle individuelle (couvert F-DT-10).
