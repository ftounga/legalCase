# Mini-spec — F-FA-15 / SF-FA-15-01 Récompenses (art. 1437 et s. Cciv) — BACKEND

## Objectif

Calculateur des **récompenses** dues entre la communauté et un patrimoine propre lors de la liquidation d'un régime communautaire (art. 1437 et s. Cciv). Cœur technique de la liquidation. Algorithme complexe du **profit subsistant** (art. 1469 al. 3 Cciv).

Outil **single-country FR DROIT_FAMILLE**. Le droit belge a sa propre logique d'indemnités (art. 1432-1438 anciens C. civ. BE) — hors scope de cette SF.

## Comportement nominal

À partir d'une liste d'**opérations** (dépense propre au profit de la communauté ou inverse), le calculator détermine pour chaque opération :
- la **règle applicable** (art. 1469 al. 1 / 2 / 3) selon la **nature du bien** financé
- le **montant de la récompense** dû
- la **direction** (communauté → propre ou propre → communauté)
- la **base juridique** précise + **formule** détaillée

Puis il agrège les totaux : récompenses dues par la communauté, récompenses dues par l'époux, solde net.

### Algorithme (art. 1469 Cciv)

| Alinéa | Cas | Règle |
|--------|-----|-------|
| **1469 al. 1** (règle générale) | Dépense quelconque (autre, dépenses non visées par al. 2 ou al. 3) | Récompense = **dépense faite** |
| **1469 al. 2** | Dépenses **usuelles / nécessaires** (entretien courant, dettes alimentaires) | Récompense = **la plus faible** des deux sommes (dépense / profit subsistant) |
| **1469 al. 3** | Dépense ayant servi à **acquérir, conserver ou améliorer** un bien | Récompense = **la plus forte** des deux sommes (dépense / profit subsistant) |

### Calcul du profit subsistant

- **ACQUISITION_BIEN_PROPRE** : profit subsistant = `valeurActuelle × (montantDepense / valeurInitialeBien)` (ratio sur prix d'achat total)
- **CONSERVATION_BIEN_PROPRE** : profit subsistant = `min(valeurActuelle, montantDepense × valeurActuelle / valeurInitiale)` (la valeur conservée — généralement la valeur actuelle plafonnée à la valeur conservée)
- **AMELIORATION_BIEN_PROPRE** : profit subsistant = `valeurActuelle - valeurInitiale` (plus-value attribuable aux travaux), borné par `valeurActuelle × montantDepense / max(valeurInitiale, 1)` quand le ratio est exploitable
- **DEPENSES_USUELLES** : profit subsistant = montantDepense (al. 2 retient la plus faible, donc revient à la dépense quand le bien n'a plus de valeur résiduelle attribuable)
- **AUTRE** : profit subsistant non calculé — al. 1 → récompense = dépense

## Cas d'erreur

- `regimeMatrimonial` = `SEPARATION_BIENS` → 400 (récompenses N/A en séparation de biens, sauf entre les patrimoines des époux — hors scope)
- `regimeMatrimonial` invalide ou null → 400
- `operations` null ou vide → réponse OK avec totaux à 0 et message "Aucune opération soumise"
- `operations[].id` null/vide → 400
- `operations[].type` invalide → 400
- `operations[].natureBien` invalide → 400
- `operations[].montantDepenseEur` négatif → 400
- `valeurInitialeBienEur` ou `valeurActuelleBienEur` négatif → 400
- Dossier non FRANCE → 400 ("Récompenses propres au droit français")
- Dossier non DROIT_FAMILLE → 400

## Critères d'acceptation

1. POST `/api/v1/case-files/{caseFileId}/recompenses` avec une opération `AMELIORATION_BIEN_PROPRE` et profit subsistant > dépense → récompense = profit subsistant.
2. POST avec une opération `AMELIORATION_BIEN_PROPRE` et profit subsistant < dépense → récompense = dépense (plus forte des 2 — al. 3).
3. POST avec une opération `DEPENSES_USUELLES` → récompense = la plus faible des 2 (al. 2).
4. POST avec une opération `AUTRE` → récompense = dépense (al. 1, règle générale).
5. POST avec opérations multiples mixtes → totaux agrégés + solde net cohérents.
6. POST avec `SEPARATION_BIENS` → 400.
7. POST sans opérations → OK avec totaux à 0.
8. GET après POST → renvoie l'analyse persistée.
9. GET sans POST préalable → 404.
10. Workspace BE → 400.
11. Domaine DROIT_DU_TRAVAIL → 400.
12. Cross-workspace access → 404.

## Plan de test minimal

- **≥ 22 UT** sur `RecompensesCalculator` :
  - ≥ 5 par alinéa de 1469 (al. 1 / al. 2 / al. 3)
  - Edge cases : régime invalide, 0 opération, opérations multiples, montants nuls, valeurs initiales nulles, propriétés négatives
  - Direction de la récompense (COMMUNAUTE_DOIT_PROPRE / PROPRE_DOIT_COMMUNAUTE)
  - Solde net agrégé
- **≥ 10 IT** sur `RecompensesControllerIT` :
  - POST nominal FR / GET FR / 400 BE / 400 DT / 404 cross-WS / 400 SEPARATION_BIENS / upsert / 0 opération / régime invalide / multi-opérations agrégées
- Isolation workspace toujours testée (cross-WS = 404)

## Tables / endpoints / composants impactés

### Endpoints
- POST `/api/v1/case-files/{caseFileId}/recompenses`
- GET `/api/v1/case-files/{caseFileId}/recompenses`

### Tables
- `recompenses_analyses` (nouvelle, migration **138**)
  - `id` UUID PK
  - `case_file_id` UUID FK unique → case_files
  - `regime_matrimonial` varchar(40) NOT NULL
  - `operations_json` text NOT NULL (sérialisation des opérations soumises)
  - `country` varchar(20) NOT NULL
  - `result_data` text NOT NULL (sérialisation `RecompensesResult`)
  - `created_at`, `updated_at` timestamptz NOT NULL

### Visibility rule
- UUID `f1a04001-0000-0000-0000-ee00000fa151`
- Layer ALWAYS_ON, FR + DROIT_FAMILLE
- Tool id `F-FA-15-recompenses`
- Priority `76`

### Composants Java
- `RecompensesCalculator` (algorithme art. 1469)
- `RecompensesRequest` / `RecompensesResponse` / `RecompensesResult` / `OperationRecompense` / `RecompenseDetail` (records)
- `RecompensesAnalysis` (entity)
- `RecompensesRepository` (Spring Data)
- `RecompensesService` (orchestration + persistance)
- `RecompensesController` (POST + GET)

## Hors périmètre

- **Frontend** (SF-FA-15-02 planifiée vague suivante)
- Régime belge des indemnités (logique différente — featuring distinct futur si besoin)
- **SEPARATION_BIENS** entre époux (créances entre patrimoines, hors logique récompenses)
- Indemnité d'occupation, comptes de gestion, dettes vis-à-vis de tiers
- Actualisation des récompenses (non prévue par 1469 — la valeur retenue est celle au jour de la liquidation)

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/recompenses`

Request :
```json
{
  "regimeMatrimonial": "COMMUNAUTE_LEGALE",
  "operations": [
    {
      "id": "op-1",
      "type": "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE",
      "natureBien": "AMELIORATION_BIEN_PROPRE",
      "montantDepenseEur": 50000.00,
      "valeurInitialeBienEur": 200000.00,
      "valeurActuelleBienEur": 350000.00,
      "description": "Travaux d'agrandissement maison"
    }
  ]
}
```

Response :
```json
{
  "caseFileId": "uuid",
  "regimeMatrimonial": "COMMUNAUTE_LEGALE",
  "recompenses": [
    {
      "operationId": "op-1",
      "type": "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE",
      "natureBien": "AMELIORATION_BIEN_PROPRE",
      "regleApplicable": "PROFIT_SUBSISTANT_PLUS_FORTE",
      "depenseFaiteEur": 50000.00,
      "profitSubsistantEur": 150000.00,
      "montantRecompenseEur": 150000.00,
      "directionRecompense": "COMMUNAUTE_DOIT_PROPRE",
      "baseJuridiqueOperation": "Art. 1469 al. 3 Cciv (acquisition/conservation/amélioration)",
      "formule": "max(50000 ; 150000) = 150000 (profit subsistant retenu — plus-value 350000-200000)",
      "description": "Travaux d'agrandissement maison"
    }
  ],
  "totalRecompensesDuesParCommunauteEur": 150000.00,
  "totalRecompensesDuesParEpouxEur": 0.00,
  "soldeNetPourEpouxEur": 150000.00,
  "baseJuridiqueGenerale": "Art. 1437 et 1469 Cciv",
  "messages": [
    "Profit subsistant retenu (méthode 1469 al. 3) car dépense ayant servi à amélioration",
    "..."
  ],
  "country": "FRANCE"
}
```

### Enums

- `regimeMatrimonial` : `COMMUNAUTE_LEGALE`, `PARTICIPATION_AUX_ACQUETS`, `COMMUNAUTE_UNIVERSELLE` (récompenses applicables) ; `SEPARATION_BIENS` → 400
- `type` : `DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE`, `DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE`
- `natureBien` : `ACQUISITION_BIEN_PROPRE`, `CONSERVATION_BIEN_PROPRE`, `AMELIORATION_BIEN_PROPRE`, `DEPENSES_USUELLES`, `AUTRE`
- `regleApplicable` : `DEPENSE_FAITE` (al. 1), `PLUS_FAIBLE_DEPENSE_OU_PROFIT` (al. 2), `PROFIT_SUBSISTANT_PLUS_FORTE` (al. 3)
- `directionRecompense` : `COMMUNAUTE_DOIT_PROPRE`, `PROPRE_DOIT_COMMUNAUTE`

## Exemples chiffrés

### Exemple 1 — Amélioration bien propre (art. 1469 al. 3)

Mariage en communauté légale. Mme apporte une maison **bien propre** (héritage), valeur 200 000 € au jour de l'apport. La communauté finance des travaux d'agrandissement pour 50 000 €. Au jour de la liquidation, le bien vaut 350 000 €.

- Plus-value attribuable aux travaux : 350 000 - 200 000 = **150 000 €**
- Profit subsistant = 150 000 €
- Dépense faite = 50 000 €
- Al. 3 → récompense = max(50 000 ; 150 000) = **150 000 €** dus par Mme à la communauté.
- Ici la dépense fut **propre au profit de la communauté** ? Non, la dépense est de la **communauté au profit du propre** → Mme doit récompense à la communauté de **150 000 €**.

### Exemple 2 — Acquisition bien propre par fonds communs (art. 1469 al. 3)

Mme achète un appartement en propre (clause d'emploi) pour **300 000 €**, dont **60 000 €** financés par la communauté. Au jour de la liquidation, l'appartement vaut **420 000 €**.

- Profit subsistant = 420 000 × 60 000 / 300 000 = **84 000 €**
- Dépense faite = 60 000 €
- Al. 3 → récompense = max(60 000 ; 84 000) = **84 000 €** dus par Mme à la communauté.

### Exemple 3 — Dépense usuelle (art. 1469 al. 2)

M. utilise des deniers propres (5 000 €) pour régler une dette personnelle dont la communauté a profité (charge ménagère antérieure au mariage assumée par la communauté). Hypothèse : aucune valorisation (dépense consommée).

- Dépense faite = 5 000 €
- Profit subsistant calculé = 5 000 € (consommée — pas de valeur résiduelle)
- Al. 2 → récompense = min(5 000 ; 5 000) = **5 000 €** dus par la communauté à M.

Variante : si le profit subsistant retenu est seulement de 3 000 €, la récompense passe à **3 000 €** (la plus faible).

### Exemple 4 — Conservation bien propre (art. 1469 al. 3)

M. possède en propre une maison estimée 250 000 €. Faute de paiement d'un crédit, la maison est saisie. La communauté paie **40 000 €** pour éteindre le crédit. Au jour de la liquidation, la maison vaut **300 000 €**.

- Profit subsistant = valeur conservée = **300 000 €** (la valeur du bien sauvegardé) plafonnée à la part conservée. Pour ratio simple : 300 000 × 40 000 / 250 000 = **48 000 €**.
- Dépense faite = 40 000 €
- Al. 3 → récompense = max(40 000 ; 48 000) = **48 000 €** dus par M. à la communauté.

### Exemple 5 — Dépense ordinaire / autre (art. 1469 al. 1)

M. paie 8 000 € de fonds propres pour les vacances de la famille. Aucune valeur n'est attribuable au patrimoine commun.

- Dépense faite = 8 000 €
- Profit subsistant non pertinent → al. 1 → récompense = **8 000 €** dus par la communauté à M.

### Exemple 6 — Cas multiples agrégés

- Op 1 : amélioration bien propre M. (50 000 € dépense communauté → profit subsistant 150 000 €) → 150 000 € dus par M. à la communauté.
- Op 2 : dépense usuelle M. (8 000 € dépense propre M.) → 8 000 € dus par la communauté à M.
- Op 3 : acquisition bien propre Mme (60 000 € dépense communauté, profit 84 000 €) → 84 000 € dus par Mme à la communauté.

Totaux :
- Récompenses dues par la communauté = 8 000 €
- Récompenses dues par les époux à la communauté = 150 000 + 84 000 = 234 000 €
- **Solde net pour les époux** = 8 000 - 234 000 = **-226 000 €** (les époux doivent à la communauté).

> **Note** : `totalRecompensesDuesParEpouxEur` agrège ici les deux époux (M. + Mme) — le détail par époux est conservé via `directionRecompense` mais hors-scope d'une ventilation par individu (l'opération attribue déjà le bien à un patrimoine propre via la nature de l'opération).

## Analyse de cohérence transversale

- **Autres outils domaine famille** : F-FA-08 (prestation compensatoire), F-FA-09 (divorce faute), F-FA-10 (divorce alteration), F-FA-11 (divorce accepté), F-FA-12 (divorce désunion BE) — tous calculator + endpoint sous case-file. **Pattern aligné**.
- **Pays** : FR uniquement. Régime BE des reprises/indemnités (art. 1432-1438 anciens C. civ. BE) → backlog futur si demande client. Justifié.
- **Règle "un outil = une situation"** : Récompenses ≠ liquidation globale ≠ indemnité d'occupation. Cet outil traite **uniquement** les récompenses 1437/1469. Aucun autre outil ne couvre ce périmètre.
- **Pattern UI partagé** : N/A (backend SF).
- **Composants partagés** : utilise `decision_tool_visibility_rules` (F-IA-04). Aucun nouveau pattern transverse introduit.

## Impact par domaine métier

Cette feature est **spécifique au droit de la famille FR** (régimes matrimoniaux civilistes art. 1400 et s. Cciv).
- **Droit du travail** : non applicable.
- **Immigration** : non applicable.
- **Droit famille FR** : cœur fonctionnel.
- **Droit famille BE** : non couvert (régime juridique différent — backlog F-FA-15-BE potentiel).

Aucun risque d'asymétrie 3 domaines : la liquidation matrimoniale n'a pas d'équivalent hors droit famille.

## Préoccupations transversales

- Auth / Principal : aucune modification — utilise `OidcUser` + `Principal` standard.
- Workspace context : standard (filtre `workspace_id`, `WorkspaceMemberRepository`).
- Plans / limites : aucun (l'outil ne consomme pas de quota IA).
- Routing / navigation : N/A (backend).
- Outil décisionnel métier : nouveau, isolé. Visibility rule ajoutée.

Aucune préoccupation transversale critique à propager.
