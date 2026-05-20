# SF-212-43 — Backend : outil décisionnel « particulier employeur — CESU / garde d'enfants »

> Feature F-212. Outil : `F-DT-108-particuliers-employeurs-cesu`. Fondement : CCN des salariés du particulier employeur (IDCC 2111) du 24/11/1999 révisée 2021 ; CCN des assistants maternels du particulier employeur (IDCC 2395) ; L. 1271-1+ CT (chèque emploi service universel — CESU).

## Objectif

Fournir le moteur backend qui calcule les droits d'un salarié du particulier employeur (garde d'enfants, aide ménagère, aide à domicile) : préavis, indemnité de licenciement, congés payés — soumis à un régime distinct du droit commun.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/particulier-employeur-cesu` + `GET`.

**CCN particulier employeur** (IDCC 2111) :
- **Préavis** : 1 mois si ancienneté ≥ 6 mois mais < 2 ans ; 2 mois si ≥ 2 ans (art. 10 CCN).
- **Indemnité de licenciement** : 1/4 mois/an pour les 10 premières années + 1/3 mois/an au-delà (plus favorable que droit commun).
- **Congés payés** : 2,5 jours ouvrables par mois travaillé (même règle L. 3141-3).
- **CESU** : si emploi via CESU préfinancé, vérifier exonérations.
- **Majoration nuit/dimanche/jours fériés** : selon CCN.

**CCN assistants maternels** (IDCC 2395) : régime distinct (agrément, accueil domicile, indemnité d'entretien).

Verdict `CalculParticulierEmployeur` : calcul préavis + IL + CP + total indemnités dues.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-44)

```
POST /api/v1/case-files/{caseFileId}/particulier-employeur-cesu
Request {
  typeCcn: enum PARTICULIER_EMPLOYEUR|ASSISTANT_MATERNEL,
  ancienneteMois: int,
  salaireMensuelBrutEuros: double,
  heuresHebdoMoyennes: double,
  typeRupture: enum LICENCIEMENT|DEMISSION|RETRAITE|AUTRE,
  congesPayesNonPrisJours: int
}
Response 200 {
  ...inputs (snapshot),
  dureePreavisJours: int,
  indemnitePreavisEuros: double,
  indemniteLicenciementCcnEuros: double,
  indemniteCongesPayesEuros: double,
  totalIndemnitesEuros: double,
  ccnApplicable: String,
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/particulier-employeur-cesu → 200 | 204
```

`critereCode` F-IA-03 : `DT108_TYPE_CCN`, `DT108_ANCIENNETE`, `DT108_SALAIRE`, `DT108_TYPE_RUPTURE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `particulier_employeur_detail` :
`particulierEmployeurCcn`, `particulierEmployeurAncienneteMois`, `particulierEmployeurSalaire`, `particulierEmployeurTypeRupture`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Ancienneté 18 mois + licenciement → préavis 1 mois, IL CCN calculée.
2. Ancienneté 3 ans + licenciement → préavis 2 mois, IL CCN > IL légale.
3. Démission → préavis 1 ou 2 mois, IL = 0.
4. CCN assistant maternel → `ccnApplicable = "CCN assistants maternels (IDCC 2395)"`.
5. 422 hors `FRANCE`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-108-particuliers-employeurs-cesu` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `ParticulierEmployeurCesuCalculatorTest`** : ancienneté 18 m vs 3 ans ; IL CCN vs légale ; démission.
- **IT `ParticulierEmployeurCesuControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `particulier_employeur_cesu_analyses`.
- **Seed** : `tool_id=F-DT-108-particuliers-employeurs-cesu`, `trigger_field=particulier_employeur_detecte`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-44). CESU préfinancé (exonérations fiscales — hors périmètre V1). Assistants maternels régime agrément (P3, F-218).
