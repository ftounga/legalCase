# Mini-spec — F-207 / SF-207-07-backend Outil RCC BE indemnité complémentaire

## Identifiant

`F-207 / SF-207-07-backend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-07-backend-rcc-be-indemnite-complementaire`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern source : calculateur BE (SF-207-01 PrescriptionBe, SF-207-04 AtFedris) + RCC conditions SF-207-06 pour la cohérence du périmètre RCC.

## Objectif

Calculateur de l'**indemnité complémentaire RCC** (Régime de Chômage avec Complément d'entreprise BE) — détermine le montant mensuel dû par l'employeur au salarié RCC = **différentiel entre 95 % de la rémunération nette de référence et l'allocation ONEM** (CCT 17 art. 5 ; CCT sectorielles éventuellement plus favorables). Outil BE-only.

## Substance juridique (BE strict)

- **CCT 17** (régime général RCC) art. 5 : l'indemnité complémentaire est égale à la moitié de la différence entre la rémunération nette de référence et l'allocation de chômage.
- Formule pratique simplifiée : `indemniteEmployeur = (remunerationNetteReference - allocationOnemMensuelle) / 2`.
- Plancher : indemnité ≥ 0 (jamais à charge du salarié).
- Mode « CCT sectorielle » : si l'utilisateur indique un **plancher sectoriel** (% ou montant fixe), l'indemnité ne peut pas être inférieure à ce plancher.
- L'indemnité est due **mensuellement** jusqu'à l'âge légal de la pension (66 ans en 2026, 67 ans à partir de 2030 — paramètre `ageLegalPension`).

## Contrat API

`POST /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-indemnite-complementaire`

Inputs (`RccBeIndemniteRequest`) :
```json
{
  "remunerationNetteReference": 3200.00,           // requis, BigDecimal mensuel
  "allocationOnemMensuelle": 1500.00,              // requis, BigDecimal mensuel
  "dateNaissanceSalarie": "1964-05-15",            // requis (ISO) — base calcul mois restants jusqu'à pension
  "dateDebutRcc": "2026-09-01",                    // requis (ISO) — date début effective du RCC
  "ageLegalPension": 66,                           // optionnel, default 66 (2026)
  "planchersSectoriel": null                       // optionnel, BigDecimal — CCT sectorielle si plus favorable
}
```

Réponse 200 :
```json
{
  "indemniteMensuelleEmployeur": 850.00,           // (remunNette - allocOnem) / 2, ≥ planchersSectoriel
  "indemniteMensuelleAvantPlancher": 850.00,       // formule CCT 17 art. 5 brute
  "planchersSectorielApplique": false,
  "moisRestantsJusquaPension": 39,                 // mois entre dateDebutRcc et 66e anniversaire
  "montantTotalEmployeur": 33150.00,               // indemniteMensuelleEmployeur × moisRestants
  "remunerationNetteReference": 3200.00,
  "allocationOnemMensuelle": 1500.00,
  "baseJuridique": "CCT 17 art. 5 ; loi du 3 juillet 1978 ; arrêté royal du 3 mai 2007",
  "formuleCalcul": "(3200,00 € - 1500,00 €) / 2 = 850,00 € / mois × 39 mois = 33 150,00 €. Plancher sectoriel non applicable."
}
```

## Logique de calcul (`RccBeIndemniteCalculator`)

1. `indemniteMensuelleAvantPlancher = (remunNetteReference - allocationOnemMensuelle) / 2`, plancher 0 €.
2. `indemniteMensuelleEmployeur = max(indemniteMensuelleAvantPlancher, planchersSectoriel ?? 0)`.
3. `planchersSectorielApplique = (planchersSectoriel != null && planchersSectoriel > indemniteMensuelleAvantPlancher)`.
4. `moisRestantsJusquaPension` = mois entre `dateDebutRcc` et `dateNaissanceSalarie + ageLegalPension années`, plancher 0.
5. `montantTotalEmployeur = indemniteMensuelleEmployeur × moisRestantsJusquaPension`.

Tous calculs en `BigDecimal` (précision centimes, `RoundingMode.HALF_EVEN`, 2 décimales).

`GET` même path : dernière analyse ou 404.

## Cas d'erreur

| Situation | Code |
|---|---|
| `workspaceCountry !== BELGIQUE` | 404 |
| `caseFileId` autre workspace | 404 |
| `remunerationNetteReference < 0` ou nulle | 400 |
| `allocationOnemMensuelle < 0` | 400 |
| `dateNaissanceSalarie` futur ou > 100 ans | 400 |
| `dateDebutRcc < dateNaissanceSalarie + 50 ans` (RCC accessible dès 50 ans dans certains cas exceptionnels mais < 50 ans rarissime, suspicion d'erreur) | 400 |
| `ageLegalPension` non null mais < 60 ou > 75 | 400 |
| `planchersSectoriel` non null mais < 0 | 400 |

## Composants à créer (pattern PrescriptionBe + AtFedris)

Sous `backend/src/main/java/fr/ailegalcase/casefile/` :
- `RccBeIndemniteAnalysis.java`
- `RccBeIndemniteRepository.java`
- `RccBeIndemniteRequest.java` (Bean Validation : tous champs `@NotNull` sauf optionnels, `@DecimalMin("0")` sur les montants)
- `RccBeIndemniteResult.java` (record sans enum verdict — calculateur pur)
- `RccBeIndemniteResponse.java`
- `RccBeIndemniteCalculator.java` (fonction pure BigDecimal)
- `RccBeIndemniteService.java`
- `RccBeIndemniteController.java`

Migration `XXX-create-rcc-be-indemnite-analyses.xml` (prochain après 263). Table standard.

Extensions :
- `LegalDomainPromptBuilder` BE Travail : ajout 3 champs IA `remunerationNetteReferenceRccDetectee` (BigDecimal/Double), `allocationOnemMensuelleEstimee` (BigDecimal/Double), `dateDebutRccEnvisagee` (String ISO). `dateNaissanceSalarie` déjà extrait par SF-207-06.
- `CaseAnalysisResponse.TravailExtractedData` : ajout ces 3 fields. Rétrocompat Builder.

## Critères d'acceptation

- [ ] Cas nominal — `remunNette=3200, allocOnem=1500` → `indemnite=850`.
- [ ] `remunNette ≤ allocOnem` → `indemnite=0` (plancher 0).
- [ ] `planchersSectoriel=1000` avec `indemnite calculée=850` → `indemnite=1000`, `planchersSectorielApplique=true`.
- [ ] `moisRestants` = différence date début RCC vs 66ᵉ anniversaire en mois.
- [ ] `dateDebutRcc` après l'âge de pension → `moisRestants=0`, `montantTotal=0`.
- [ ] Précision BigDecimal : `(3201.55 - 1500.20) / 2 = 850.68` (HALF_EVEN, 2 décimales).
- [ ] Workspace FR → 404, autre workspace → 404.
- [ ] Validation Bean : montants négatifs → 400.
- [ ] `GET` après `POST` → 200.
- [ ] `critereCode` BE_RCC_INDEMNITE_REMUN_REFERENCE, BE_RCC_INDEMNITE_ALLOCATION_ONEM, BE_RCC_INDEMNITE_DATE_DEBUT émis. CritereCodeIntegrityIT vert.

## Hors scope

- Frontend (SF-207-07b).
- Calcul détaillé de l'allocation ONEM (formule complexe ONEM, hors scope — l'avocat fournit le montant déjà calculé).
- Cotisations sociales sur l'indemnité (régime fiscal/parafiscal RCC) — autre outil potentiel V2.
- Indemnité de licenciement RCC (différente de l'indemnité complémentaire — confusion à éviter).

## Plan de test

`RccBeIndemniteCalculatorTest` (10+ tests : nominal, plancher 0, plancher sectoriel, BigDecimal précision, moisRestants en bornes, dateDebut futur, etc.).
`RccBeIndemniteControllerIT` (5+ tests : BE OK, FR 404, autre workspace 404, validation 400, GET 404).

## Dépendances

- SF-207-06 backend (#1151) — `dateNaissanceSalarie` déjà dans `TravailExtractedData`.
- Pattern PrescriptionBe / AtFedris pour les calculateurs purs sans verdict.
