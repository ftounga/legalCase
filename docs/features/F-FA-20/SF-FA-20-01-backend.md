# Mini-spec — F-FA-20 / SF-FA-20-01 Dissolution PACS — BACKEND

## Objectif

Outil FR **dissolution du PACS** (art. 515-7 et 515-7-1 Cciv). Calcul de la
validité de la dissolution selon le mode (déclaration unilatérale/conjointe,
mariage, décès), de l'éligibilité aux créances entre partenaires (art. 515-8),
du score de probabilité du contentieux et du verdict de recommandation
(liquidation amiable / médiation ou JAF / contentieux inévitable).

## Règles juridiques

- Modes de dissolution (art. 515-7 Cciv) :
  - `DECLARATION_UNILATERALE` : signification par huissier au partenaire
    (notification obligatoire + remise au greffe)
  - `DECLARATION_CONJOINTE` : déclaration commune au greffe
  - `MARIAGE_PARTENAIRES` : mariage des deux partenaires entre eux
  - `MARIAGE_TIERS` : mariage de l'un avec un tiers
  - `DECES` : décès d'un partenaire
- Régimes de biens (art. 515-5 et 515-5-1 Cciv) :
  - `SEPARATION_BIENS` (régime légal du PACS depuis 2007)
  - `INDIVISION_AMENAGEE` (option choisie ≥ 2007)
  - `INDIVISION_PAR_DEFAUT` (PACS conclus avant 2007 par défaut)
- Créances entre partenaires (art. 515-7-1 et 515-8 Cciv, jurisprudence) :
  - `CONTRIBUTION_DESEQUILIBRE` : contribution aux charges déséquilibrée
  - `INVESTISSEMENT_BIEN_PROPRE` : investissement dans un bien propre
  - `ENRICHISSEMENT_INJUSTE` : enrichissement sans cause (art. 1303 Cciv)
  - `PRESTATION_TRAVAIL_NON_REMUNEREE` : aide professionnelle non rémunérée
  - `AUCUNE`
- Prescription des actions en créances : 5 ans (art. 2224 Cciv)
- Pas de prestation compensatoire entre partenaires (régime exclusif des
  époux — voir F-FA-08/09/10)

## Inputs

- `dateConclusionPacs` : LocalDate (date de conclusion du PACS)
- `modeDissolution` : enum `ModeDissolution`
- `dateDissolution` : LocalDate
- `dureeUnionAnnees` : int
- `regimeBiens` : enum `RegimeBiens`
- `patrimoineCommunSignificatif` : boolean
- `creancesAlleguees` : List<enum `CreanceType`>
- `enfantsCommuns` : int
- `dateNotificationPartenaire` : LocalDate (requis si DECLARATION_UNILATERALE)

## Outputs

- `caseFileId` : UUID + écho des inputs
- `dissolutionValide` : boolean (selon mode + cohérence dates)
- `delaiNotificationOk` : boolean (= dateNotif ≥ dateDissolution si UNILATERALE)
- `dureeUnionEligibleCreances` : boolean (= ≥ 1 an)
- `scoreCreancesProbables` : int 0-100
- `verdictRecommandation` : enum
- `creancesPotentielleVisibles` : List<enum>
- `delaiPrescriptionAnnees` : int (= 5)
- `baseJuridique` : "Art. 515-7+515-7-1+515-8 Cciv"
- `formule`
- `messages` : explications, prescription, conseils
- `country` : "FRANCE"

## Logique

### Validité dissolution
- `MARIAGE_PARTENAIRES` / `MARIAGE_TIERS` / `DECES` → automatique, valide
- `DECLARATION_CONJOINTE` → valide (greffe)
- `DECLARATION_UNILATERALE` → valide si `dateNotificationPartenaire` ≥
  `dateDissolution` (signification huissier)

### Éligibilité créances
- `dureeUnionEligibleCreances` = `dureeUnionAnnees` ≥ 1 (seuil
  jurisprudentiel pour caractériser un déséquilibre durable)

### Score créances probables (0-100)
- Patrimoine commun significatif : +30
- ≥ 2 créances alléguées : +25
- Durée union ≥ 3 ans : +20
- Régime ≠ SEPARATION_BIENS : +15
- Enfants communs ≥ 1 : +10

### Verdict
- ≥ 70 → `CONTENTIEUX_INEVITABLE`
- 40-69 → `MEDIATION_OU_JAF`
- 1-39 → `LIQUIDATION_AMIABLE`
- 0 → `RIEN_A_FAIRE`

### Créances visibles
- Filtrer `creancesAlleguees` en retirant `AUCUNE`. Si union < 1 an → liste
  vide (recommandation).

## Architecture

- Pattern référence : `DivorceAccepteCalculator` + `RecompensesCalculator`
- Single-country FR + DROIT_FAMILLE
- Migration **152** : table `pacs_dissolution_analyses`
- Visibility rule UUID `f1a04001-0000-0000-0000-ee00000fa201`,
  ALWAYS_ON FRANCE DROIT_FAMILLE, priority 81, tool_id
  `F-FA-20-pacs-dissolution`

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/pacs-dissolution`

Voir bloc contrat dans le ticket parent.

## Tests

- ≥ 14 UT (modes dissolution, scoring tous brackets, verdicts, prescription,
  validations dates, listes créances)
- ≥ 8 IT (POST FR nominal, gates BE / DROIT_DU_TRAVAIL / autre workspace,
  upsert, GET 404, GET après POST, body invalide)

## Hors scope

- Frontend (SF-FA-20-02)
- PACS belge / cohabitation légale BE (loi 23/11/1998) — feature distincte
  ou SF future. Le seul gate est `country=FRANCE`.

## Impact par domaine métier

**Sensible au domaine** : oui (DROIT_FAMILLE).
**Sensible au pays** : oui (FRANCE seulement). Le PACS BE équivalent
(cohabitation légale) sera traité dans une SF future si besoin (déjà cité
dans la fiche F-FA-20 du PRODUCT_SPEC). Travail / Immigration : non
applicable.

## Parité des domaines métier

Niveau 5 (scoring) — outil applicable uniquement au DROIT_FAMILLE FR.
Travail / Immigration : non applicable. Famille BE (cohabitation légale) :
backlog (F-FA-20 mentionne BE). Le présent ticket ne livre que la partie FR.

## Analyse de cohérence transversale

Aucun nouveau composant partagé. Service + DTO + entity propres au calculateur,
suivant le pattern F-FA-08/09/10. Pas d'impact sur d'autres outils.
