# SF-212-05 — Backend : outil décisionnel « transfert d'entreprise — maintien des contrats L. 1224-1 »

> Feature F-212. Outil : `F-DT-72-transfert-entreprise-l1224-1`. Fondement : L. 1224-1 CT ; jurisprudence Cass. soc. sur la notion d'entité économique autonome (EEA) ; Directive 2001/23/CE.

## Objectif

Fournir le moteur backend qui analyse si les conditions d'application de L. 1224-1 sont réunies lors d'un transfert d'entreprise (maintien automatique de plein droit des contrats de travail au repreneur) et détecte les irrégularités éventuelles.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/transfert-entreprise-l1224-1` + `GET`.

L'analyseur vérifie :
- **Entité économique autonome (EEA)** : ensemble organisé de personnes et d'éléments corporels et incorporels, permettant l'exercice d'une activité économique poursuivant un objectif propre (Cass. soc. 18/07/2000 CGEA).
- **Transfert** : cession, fusion, apport partiel d'actif, externalisation, reprise d'activité.
- **Maintien automatique** (L. 1224-1) : tous les contrats de travail en cours au moment du transfert sont maintenus de plein droit au repreneur, avec toutes leurs clauses.
- **Irrégularités** : licenciements prononcés par le cédant avant le transfert pour permettre le rachat (frauduleux) ; non-information des salariés (L. 1224-3) ; non-maintien de clauses contractuelles.
- **Information-consultation du CSE** (L. 1224-3, L. 2323-1 et suivants) : obligation employeur avant le transfert.

Verdict `AnalyseTransfert` : `L1224_APPLICABLE` / `L1224_INAPPLICABLE` / `L1224_INCERTAIN` + liste des `PointAnalyseTransfert(code, libelle, fondement, conclusion)`. Détection des licenciements potentiellement frauduleux pre-transfert.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-06)

```
POST /api/v1/case-files/{caseFileId}/transfert-entreprise-l1224-1
Request {
  typeTransfert: enum CESSION|FUSION|APPORT_PARTIEL_ACTIF|EXTERNALISATION|REPRISE_ACTIVITE|AUTRE,
  eeaIdentifieeAvantTransfert: boolean,
  activiteEconomiquePreservee: boolean,
  salariesTransferes: boolean,
  contratsModifiesParRepreneur: boolean,
  licenciementsPreTransfert: boolean,
  nbLicenciementsPreTransfert: int,
  informationConsultationCseRealisee: boolean,
  dateTransfert: LocalDate|null
}
Response 200 {
  ...inputs (snapshot),
  analyseL1224_1: L1224_APPLICABLE|L1224_INAPPLICABLE|L1224_INCERTAIN,
  scoreApplicabilite: int,
  pointsAnalyse: [{code, libelle, fondement, conclusion}],
  alerteLicenciementsFrauduleux: boolean,
  alerteDefautConsultationCse: boolean,
  basesJuridiques: [String],
  messages: [String],
  calculatedAt: Instant
}
GET …/transfert-entreprise-l1224-1 → 200 | 204
```

`critereCode` F-IA-03 : `DT72_EEA_IDENTIFIEE`, `DT72_ACTIVITE_PRESERVEE`, `DT72_LICENCIEMENTS_PRE_TRANSFERT`, `DT72_CONSULTATION_CSE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `transfert_entreprise_detail` :
`transfertTypeTransfert`, `transfertEeaIdentifiee`, `transfertActivitePreservee`, `transfertLicenciementsPreTransfert`, `transfertDateTransfert`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. EEA identifiée + activité préservée → `L1224_APPLICABLE`.
2. EEA non identifiée → `L1224_INAPPLICABLE`.
3. Licenciements pré-transfert détectés → `alerteLicenciementsFrauduleux = true`.
4. Consultation CSE non réalisée → `alerteDefautConsultationCse = true`.
5. 422 hors `DROIT_DU_TRAVAIL`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-72-transfert-entreprise-l1224-1` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `TransfertEntrepriseL12241CalculatorTest`** : chaque type de transfert ; EEA ; licenciements frauduleux.
- **IT `TransfertEntrepriseL12241ControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `transfert_entreprise_l1224_1_analyses`.
- **Seed** `decision_tool_visibility_rules` : `tool_id=F-DT-72-transfert-entreprise-l1224-1`, `layer=CONTEXTUAL`, `trigger_field=transfert_entreprise_detecte`, `trigger_value=true`.
- **Nouveaux fichiers** + modifications standard.

## Hors périmètre

- Frontend (→ SF-212-06).
- Droit social de la fusion au niveau du groupe (droit des sociétés, hors périmètre Travail FR V1).
