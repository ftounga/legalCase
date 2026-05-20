# SF-212-17 — Backend : outil décisionnel « rupture anticipée CDD »

> Feature F-212. Outil : `F-DT-43-rupture-anticipee-cdd`. Fondement : L. 1243-1 à L. 1243-4 CT.

## Objectif

Fournir le moteur backend qui analyse la légalité d'une rupture anticipée de CDD et calcule les indemnités dues, selon que la rupture est le fait de l'employeur ou du salarié, et selon le motif invoqué.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/rupture-anticipee-cdd` + `GET`.

Motifs légaux de rupture anticipée (L. 1243-1 à L. 1243-3) :
- **Accord des parties** (L. 1243-1) : pas d'indemnité spécifique hors accord.
- **Faute grave** du salarié (L. 1243-1) : pas d'indemnité de rupture anticipée.
- **Force majeure** (L. 1243-1) : pas d'indemnité (sauf maintien salaire chômage technique L. 5122-1).
- **Inaptitude médicale** (L. 1226-4 applicable au CDD via L. 1243-1).
- **Embauche en CDI** : le salarié peut rompre avant terme si embauche CDI ailleurs (L. 1243-2).

**Cas irréguliers** — sanctions L. 1243-4 :
- Rupture par l'employeur sans motif légal : indemnité égale aux **salaires que le salarié aurait perçus jusqu'au terme** + indemnité de précarité (10 % rémunération totale).
- Rupture par le salarié sans motif légal : dommages-intérêts à l'employeur = préjudice réel.

Verdict `AnalyseRuptureAnticipeeCdd` : `LEGITIME` / `ILLEGITIME_EMPLOYEUR` / `ILLEGITIME_SALARIE` + calcul indemnités.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `type_contrat` du dossier ≠ `CDD` → 422 (outil CDD uniquement).
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-18)

```
POST /api/v1/case-files/{caseFileId}/rupture-anticipee-cdd
Request {
  auteurRupture: enum EMPLOYEUR|SALARIE,
  motifRupture: enum ACCORD_PARTIES|FAUTE_GRAVE|FORCE_MAJEURE|INAPTITUDE|CDI_EMBAUCHE|AUTRE,
  dateTermeCdd: LocalDate,
  dateRupture: LocalDate,
  salaireMensuelBrutEuros: double,
  remunerationBruteTotaleContratEuros: double
}
Response 200 {
  ...inputs (snapshot),
  analyseRuptureAnticipee: LEGITIME|ILLEGITIME_EMPLOYEUR|ILLEGITIME_SALARIE,
  moisRestantsContrat: int,
  indemnitesRuptureAnticipeeEuros: double,
  indemnitePrecariiteEuros: double,
  totalDuSalarieEuros: double,
  basesJuridiques: [String],
  messages: [String],
  calculatedAt: Instant
}
GET …/rupture-anticipee-cdd → 200 | 204
```

`critereCode` F-IA-03 : `DT43_MOTIF_RUPTURE`, `DT43_AUTEUR_RUPTURE`, `DT43_DATE_TERME`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `rupture_anticipee_cdd_detail` :
`ruptureAnticipeeCddAuteur`, `ruptureAnticipeeCddMotif`, `ruptureAnticipeeCddDateTerme`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Rupture employeur sans motif légal → `ILLEGITIME_EMPLOYEUR`, indemnités = salaires restants + précarité.
2. Rupture faute grave salarié → `LEGITIME`, indemnités = 0.
3. Rupture accord parties → `LEGITIME`, indemnités = 0 sauf accord.
4. CDI embauche salarié → `LEGITIME`, pas de DI.
5. 422 si `type_contrat ≠ CDD`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-43-rupture-anticipee-cdd` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `RuptureAnticipeeCddCalculatorTest`** : chaque motif × chaque auteur ; calcul indemnités.
- **IT `RuptureAnticipeeCddControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `rupture_anticipee_cdd_analyses`.
- **Seed** : `tool_id=F-DT-43-rupture-anticipee-cdd`, `trigger_field=rupture_anticipee_cdd_detectee`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-18). Requalification CDD en CDI (couvert F-DT-22).
