# SF-212-41 — Backend : outil décisionnel « VRP — statut et indemnité de clientèle »

> Feature F-212. Outil : `F-DT-104-vrp-statut`. Fondement : L. 7311-1 à L. 7313-18 CT ; Cass. soc. sur la détermination du statut VRP vs agent commercial vs salarié ordinaire ; accord national professionnel VRP 03/10/1975.

## Objectif

Fournir le moteur backend qui vérifie si un salarié relève du statut VRP et calcule l'indemnité de clientèle due en cas de rupture du contrat, en plus des indemnités de droit commun.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/vrp-statut` + `GET`.

**Conditions du statut VRP** (L. 7311-3) : quatre conditions cumulatives :
1. Travail pour le compte d'un ou plusieurs employeurs.
2. Exercice d'une profession de représentation.
3. Représentation exclusive (exclusivité ou quasi-exclusivité) ou non exclusive.
4. Ne pas faire d'opérations commerciales pour son propre compte.

**Indemnité de clientèle** (L. 7313-13) : en cas de rupture non imputable au VRP (licenciement sans faute grave ou rupture à l'initiative de l'employeur), le VRP a droit à une **indemnité de clientèle** distincte de l'IL, calculée en fonction du préjudice réel (perte de clientèle apportée ou développée). La jurisprudence retient souvent 2 ans de commissions comme référence.

**Clause de non-concurrence VRP** : accord ANP 1975 — contrepartie financière obligatoire (≥ 2/3 du salaire fixe + commissions sur 12 mois si < 3 ans, 1/3 si ≥ 3 ans).

Verdict `AnalyseVrpStatut` : `STATUT_VRP_CONFIRME` / `STATUT_VRP_PROBABLE` / `STATUT_VRP_IMPROBABLE` + calcul indemnité de clientèle estimée.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422 (statut VRP FR-only).
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-42)

```
POST /api/v1/case-files/{caseFileId}/vrp-statut
Request {
  professionRepresentation: boolean,
  exclusiviteOuQuasiExclusivite: boolean,
  operationsPersonnellesAbsentes: boolean,
  commissionsMoyennesAnnuellesEuros: double,
  anneesPresenteSurClientele: int,           // années de travail sur la clientèle
  typeRuptureVrp: enum LICENCIEMENT_SANS_FAUTE|DEMISSION_POUR_MOTIF_LEGITIME|FAUTE_GRAVE|AUTRE
}
Response 200 {
  ...inputs (snapshot),
  analyseVrpStatut: STATUT_VRP_CONFIRME|STATUT_VRP_PROBABLE|STATUT_VRP_IMPROBABLE,
  scoreVrpStatut: int,
  conditionsManquantes: [{code, libelle, fondement}],
  indemnitéClienteleEstimeeEuros: double,    // 0 si faute grave
  baseCalculIndemnitéClientele: String,
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/vrp-statut → 200 | 204
```

`critereCode` F-IA-03 : `DT104_PROFESSION_REPRESENTATION`, `DT104_EXCLUSIVITE`, `DT104_OPERATIONS_PERSO`, `DT104_INDEMNITECLIENTELE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `vrp_detail` :
`vrpProfessionRepresentation`, `vrpExclusivite`, `vrpCommissionsAnnuelles`, `vrpTypeRupture`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. 4 conditions + licenciement sans faute → `STATUT_VRP_CONFIRME`, indemnité de clientèle calculée.
2. Faute grave → `STATUT_VRP_CONFIRME` mais `indemnitéClienteleEstimeeEuros = 0`.
3. Opérations pour compte propre détectées → `STATUT_VRP_IMPROBABLE`.
4. 422 hors `FRANCE`.
5. Isolation workspace → 404.
6. `tool_id=F-DT-104-vrp-statut` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `VrpStatutCalculatorTest`** : chaque combinaison conditions ; faute grave ; calcul indemnité.
- **IT `VrpStatutControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `vrp_statut_analyses`.
- **Seed** : `tool_id=F-DT-104-vrp-statut`, `trigger_field=statut_vrp_detecte`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-42). Agent commercial (indépendant, hors champ Travail FR). Clause de non-concurrence VRP (calcul couvert F-DT-24 si la clause existe).
