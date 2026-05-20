# SF-212-23 — Backend : outil décisionnel « égalité salariale femmes/hommes »

> Feature F-212. Outil : `F-DT-56-egalite-salariale-femmes-hommes`. Fondement : L. 1142-7 à L. 1142-10 CT ; L. 1144-1 CT (charge de la preuve aménagée) ; loi 05/09/2018 « avenir professionnel » ; L. 1132-1 CT.

## Objectif

Fournir le moteur backend qui analyse une situation de discrimination salariale fondée sur le sexe (action individuelle du salarié) et évalue les écarts de rémunération, en tenant compte de la charge de la preuve aménagée.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/egalite-salariale-femmes-hommes` + `GET`.

L'analyseur évalue :
- **Principe égalité de rémunération** (L. 3221-2) : à travail égal, salaire égal — entre hommes et femmes mais aussi entre tout salarié dans la même situation.
- **Charge de la preuve aménagée** (L. 1144-1) : le salarié établit des faits laissant supposer la discrimination → l'employeur doit prouver que sa décision est justifiée par des éléments objectifs étrangers à toute discrimination.
- **Comparants** : le salarié doit identifier des comparants (même qualification, mêmes fonctions, même ancienneté approximative) de sexe opposé mieux rémunérés.
- **Index égalité** (L. 1142-8) : entreprises ≥ 50 salariés publient l'index — peut constituer un élément de preuve.
- **Action individuelle** : prescription 5 ans (L. 1134-5) ; non soumise au plafond Macron — dommages et intérêts non plafonnés.

Verdict `AnalyseEgaliteSalariale` : `DISCRIMINATION_PROBABLE` / `DISCRIMINATION_POSSIBLE` / `PAS_DE_DISCRIMINATION_APPARENTE` + liste des `FacteurDisparite`.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-24)

```
POST /api/v1/case-files/{caseFileId}/egalite-salariale-femmes-hommes
Request {
  sexeSalarie: enum FEMME|HOMME,
  salaireMensuelBrutSalarieEuros: double,
  ancienneteMois: int,
  qualification: String,
  nombreComparantsMieuxPayes: int,
  ecartSalaireMoyenComparantsEuros: double,
  ecartPourcentage: double,
  indexEgaliteConnu: boolean,
  scoreIndexEgalite: int|null,              // score sur 100 si connu
  justificationsEmployeurObjectives: boolean
}
Response 200 {
  ...inputs (snapshot),
  analyseEgaliteSalariale: DISCRIMINATION_PROBABLE|DISCRIMINATION_POSSIBLE|PAS_DE_DISCRIMINATION_APPARENTE,
  scoreDiscrimination: int,
  facteursDisparite: [{code, libelle, fondement, poids, explication}],
  prescriptionActionAns: int,               // 5 ans
  alerteNonPlafonnement: String,            // "Dommages non plafonnés (hors barème Macron)"
  basesJuridiques: [String],
  messages: [String],
  calculatedAt: Instant
}
GET …/egalite-salariale-femmes-hommes → 200 | 204
```

`critereCode` F-IA-03 : `DT56_ECART_SALAIRE`, `DT56_COMPARANTS`, `DT56_INDEX_EGALITE`, `DT56_JUSTIFICATIONS_OBJECTIVES`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `egalite_salariale_detail` :
`egaliteSalarialeSexeSalarie`, `egaliteSalarialeSalaireBrut`, `egaliteSalarialeAnciennete`, `egaliteSalarialeEcartPourcentage`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Écart > 20 % + comparants identifiés + pas de justification objective → `DISCRIMINATION_PROBABLE`.
2. Score index faible (< 75/100) → facteur aggravant.
3. `alerteNonPlafonnement` toujours présente dans la réponse.
4. `prescriptionActionAns = 5` (non 3 ni 12 mois).
5. 422 hors `DROIT_DU_TRAVAIL`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-56-egalite-salariale-femmes-hommes` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `EgaliteSalarialeCalculatorTest`** : écart % ; comparants ; index ; justifications.
- **IT `EgaliteSalarialeControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `egalite_salariale_analyses`.
- **Seed** : `tool_id=F-DT-56-egalite-salariale-femmes-hommes`, `trigger_field=egalite_salariale_pressentie`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-24). Discrimination licenciement (couvert F-DT-12). Index égalité côté conformité employeur (P3, F-218 `F-DT-101`).
