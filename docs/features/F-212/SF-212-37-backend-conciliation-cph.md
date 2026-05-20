# SF-212-37 — Backend : outil décisionnel « conciliation CPH (BCO/BCA) »

> Feature F-212. Outil : `F-DT-84-conciliation-cph-bca`. Fondement : R. 1454-7 à R. 1454-12 CT ; L. 1235-1 CT (barème transactions BCA) ; formulaire D4121-1 CPC.

## Objectif

Fournir le moteur backend qui prépare la phase de conciliation au Bureau de Conciliation et d'Orientation (BCO) du CPH, calcule les offres baremées (BCA) et génère la checklist procédurale.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/conciliation-cph-bca` + `GET`.

Le **BCO** est la première étape de la procédure prud'homale — obligatoire avant le Bureau de Jugement (BJ). Deux issues :
- **Conciliation** : accord homologué par le BCO → exécutoire.
- **Orientation** : renvoi au BJ ou à la formation de référé.

**Barème de transactions BCA** (L. 1235-1 al. 3) : si accord au BCO, l'indemnité peut être inférieure au barème Macron mais respecte des seuils minimaux fixés par décret selon ancienneté :
- < 2 ans : 2 mois.
- 2 à 8 ans : 3 mois.
- 8 à 15 ans : 4 mois.
- 15 à 25 ans : 8 mois.
- ≥ 25 ans : 14 mois.

L'analyseur :
- Calcule le montant minimum BCA selon ancienneté.
- Évalue l'opportunité de la conciliation vs aller au BJ (comparaison BCA minimum vs potentiel barème Macron).
- Checklist des pièces à produire au BCO (L. 1454-4 : requête introductive, pièces justificatives).

Également : flag `conciliation_cph_envisagee` ajouté à `TravailExtractedData` (nouveau flag non livré par F-205 — extension dans cette SF).

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422 (BCO FR-only).
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-38)

```
POST /api/v1/case-files/{caseFileId}/conciliation-cph-bca
Request {
  ancienneteMois: int,
  salaireMensuelBrutEuros: double,
  montantDemandesEuros: double,
  opportuniteConciliation: enum FAVORABLE|DEFAVORABLE|INCERTAIN
}
Response 200 {
  ...inputs (snapshot),
  montantMinimumBcaEuros: double,
  palierBcaApplicable: String,             // ex. "8 à 15 ans : 4 mois"
  comparaisonBcaVsMacron: String,           // texte comparatif
  checklistBco: [String],
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/conciliation-cph-bca → 200 | 204
```

`critereCode` F-IA-03 : `DT84_ANCIENNETE`, `DT84_MONTANT_DEMANDES`, `DT84_OPPORTUNITE`.

**Extension flag IA** (nouveau flag) : `conciliation_cph_envisagee` (boolean, niveau 3) ajouté dans `TravailExtractedData` + prompt.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `conciliation_cph_detail` + flag `conciliation_cph_envisagee` :
`conciliationCphAncienneteMois`, `conciliationCphSalaire`, `conciliationCphMontantDemandes`.
Extension `LegalDomainPromptBuilder` (nouveau flag + extraction champs).

## Critères d'acceptation

1. Ancienneté 10 mois → `montantMinimumBcaEuros = 2 × salaire`.
2. Ancienneté 12 ans → `palierBcaApplicable = "8 à 15 ans : 4 mois"`.
3. Checklist BCO non vide.
4. 422 hors `FRANCE`.
5. Isolation workspace → 404.
6. `tool_id=F-DT-84-conciliation-cph-bca` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `ConciliationCphBcaCalculatorTest`** : chaque palier ancienneté.
- **IT `ConciliationCphBcaControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `conciliation_cph_bca_analyses`.
- **Seed** : `tool_id=F-DT-84-conciliation-cph-bca`, `trigger_field=conciliation_cph_envisagee`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard + extension `TravailExtractedData` (nouveau flag).

## Hors périmètre

Frontend (→ SF-212-38). Génération de la requête introductive (F-98). Bureau de Jugement proprement dit.
