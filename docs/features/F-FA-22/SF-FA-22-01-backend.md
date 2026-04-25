# Mini-spec — F-FA-22 / SF-FA-22-01 Indivision post-communautaire (art. 815 Cciv + art. 1364 CPC) — BACKEND

## Objectif

Outil décisionnel d'**éligibilité au partage judiciaire d'une indivision post-communautaire** (suite à divorce ou succession) au sens des art. 815 et s. Cciv, complété par la procédure de partage de l'art. 1364 CPC. Évalue si la voie amiable reste praticable, recommande le partage judiciaire ou la licitation, calcule l'indemnité d'occupation due au sens de l'art. 815-9 al. 2 Cciv et estime le délai prévisible de la procédure devant le TJ.

Outil **single-country FR DROIT_FAMILLE**. La BE (loi du 13 août 2011 relative au partage judiciaire — art. 1207 et s. C. jud.) suit une logique distincte ; non couverte par cette SF.

## Comportement nominal

À partir des paramètres d'indivision (date d'origine, nature des biens, valeur, indivisaires, tentatives amiables, occupation, conflit) :
- score 0-100 d'éligibilité au partage judiciaire
- verdict de recommandation (`PARTAGE_AMIABLE_POSSIBLE`, `PARTAGE_JUDICIAIRE_RECOMMANDE`, `LICITATION_REQUISE`, `MEDIATION_PREALABLE`)
- montant de l'indemnité d'occupation due, le cas échéant
- recommandation d'expertise notariale si patrimoine complexe
- recommandation de licitation si bien immobilier indivis sans accord amiable possible
- estimation du délai de procédure de partage judiciaire (mois)
- base juridique + formule explicative
- messages structurés (mention 815-9 al. 2, désignation notaire art. 1364 CPC, etc.)

### Algorithme de scoring

Score = somme des contributions (plafonné à 100) :
- ≥ 2 tentatives de partage amiable produites : **+30**
- conflit ouvert entre indivisaires : **+25**
- absence de consentement au partage global : **+20**
- durée d'indivision > 2 ans (anormalement longue) : **+15**
- occupation du bien par un indivisaire : **+10**

Verdict :
- score ≥ 70 → **PARTAGE_JUDICIAIRE_RECOMMANDE**
- score 40-69 → **LICITATION_REQUISE** si `IMMOBILIER` parmi `natureBiens` et amiable impossible (≥ 2 tentatives), sinon **MEDIATION_PREALABLE**
- score < 40 + consentement global → **PARTAGE_AMIABLE_POSSIBLE**
- score < 40 sans consentement → **MEDIATION_PREALABLE**

### Indemnité d'occupation (art. 815-9 al. 2 Cciv)

Calculée si `occupationBienParUnIndivisaire == true`, sinon `0` :

```
indemnite = valeurEstimeeTotaleEur × 0.04 / 12 × (indivisionDuréeAnnees × 12) × quotePartLesee
```

Avec `quotePartLesee = (100 - max(quotesPart)) / 100` — la quote-part des indivisaires non occupants. Le taux annuel 4 % est une convention prudente (loyer locatif net moyen FR — fourchette 3 à 5 % retenue par les notaires en l'absence d'expertise).

### Recommandations dérivées

- **`expertiseNotarialeRecommandee`** = `true` si `nbIndivisaires >= 3` OU `valeurEstimeeTotaleEur > 100 000 €`.
- **`licitationRecommandee`** = `true` si `IMMOBILIER` ∈ `natureBiens` ET (verdict ∈ {PARTAGE_JUDICIAIRE_RECOMMANDE, LICITATION_REQUISE}).
- **`delaiProcedurePartageJudiciaireMois`** :
  - 12 mois si `nbIndivisaires == 2` et pas d'immobilier
  - 18 mois si immobilier OU 3 indivisaires
  - 24 mois si ≥ 4 indivisaires OU conflit ouvert + immobilier

## Cas d'erreur

- `dateOrigineIndivision` null ou postérieure à aujourd'hui → 400
- `natureBiens` null ou vide → 400
- `natureBiens[i]` invalide → 400
- `valeurEstimeeTotaleEur` null ou négative → 400
- `nbIndivisaires` < 2 ou > 20 → 400
- `quotesPart` taille ≠ `nbIndivisaires` → 400
- somme `quotesPart` ≠ 100 (tolérance ±0,5) → 400
- `quotesPart[i]` < 0 ou > 100 → 400
- `tentativesPartageAmiable` null → 400
- `tentativesPartageAmiable[i]` invalide → 400
- `indivisionDuréeAnnees` < 0 → 400
- Dossier non FRANCE → 400 ("Indivision post-communautaire : procédure propre au droit français — F-FA-22 BE non couverte")
- Dossier non DROIT_FAMILLE → 400
- Cross-workspace access → 404

## Critères d'acceptation

1. POST avec ≥2 tentatives, conflit ouvert, sans consentement, durée 3 ans, occupation true → score ≥ 80, verdict `PARTAGE_JUDICIAIRE_RECOMMANDE`.
2. POST avec consentement global, 1 tentative, durée 1 an, sans conflit → score < 40 + verdict `PARTAGE_AMIABLE_POSSIBLE`.
3. POST avec immobilier + score 50-69 + 2 tentatives → verdict `LICITATION_REQUISE`.
4. POST avec score 50-69 sans immobilier et tentatives < 2 → verdict `MEDIATION_PREALABLE`.
5. POST avec occupation true, valeur 250 000 €, 50/50, durée 3 ans → indemnité = 250 000 × 0,04 / 12 × 36 × 0,5 = 15 000 €. Vérifier formule.
6. POST avec occupation false → indemnité = 0.
7. POST avec nbIndivisaires = 3 → expertise recommandée = true.
8. POST avec valeur > 100 000 € → expertise recommandée = true.
9. POST avec immobilier + verdict judiciaire → licitation recommandée = true.
10. POST avec mobilier seulement → licitation recommandée = false.
11. POST avec workspace BE → 400.
12. POST avec workspace DROIT_DU_TRAVAIL → 400.
13. GET après POST → renvoie l'analyse persistée.
14. GET sans POST préalable → 404.
15. Cross-workspace access (auth FR sur dossier BE) → 404.
16. Upsert : 2 POST successifs sur le même dossier → un seul enregistrement, dernier résultat retourné.

## Plan de test minimal

- **≥ 14 UT** sur `IndivisionCalculator` :
  - score sur les 5 contributions (tentatives ≥2, conflit, !consentement, durée >2, occupation)
  - verdict aux trois seuils (≥70 / 40-69 / <40)
  - indemnité d'occupation calculée correctement avec quote-parts asymétriques
  - indemnité = 0 si pas d'occupation
  - expertise recommandée si nbIndivisaires ≥ 3
  - expertise recommandée si valeur > 100 000 €
  - licitation recommandée seulement si immobilier
  - délai 12/18/24 mois selon nbIndivisaires + immobilier
  - throws sur dateOrigineIndivision null ou future, natureBiens vide/invalide, valeur négative, nbIndivisaires invalide, quotesPart taille incorrecte ou somme ≠ 100, tentatives invalides, durée négative
- **≥ 8 IT** sur `IndivisionControllerIT` :
  - POST nominal FR PARTAGE_JUDICIAIRE, POST AMIABLE, POST LICITATION
  - GET après POST, GET sans POST → 404
  - Upsert (2 POST → 1 enregistrement)
  - Workspace BE → 400, DT → 400
  - Cross-workspace → 404
  - validation manquante (dateOrigineIndivision absente, valeur négative, quotesPart somme ≠ 100)

## Tables / endpoints / composants impactés

### Endpoints
- POST `/api/v1/case-files/{caseFileId}/indivision`
- GET `/api/v1/case-files/{caseFileId}/indivision`

### Tables
- `indivision_analyses` (nouvelle, migration **151**)
  - `id` UUID PK
  - `case_file_id` UUID FK unique → case_files
  - `date_origine_indivision` date NOT NULL
  - `nature_biens` varchar(200) NOT NULL (CSV des enums sérialisés en TEXT)
  - `valeur_estimee_totale_eur` numeric(14,2) NOT NULL
  - `nb_indivisaires` integer NOT NULL
  - `tentatives_partage_amiable` varchar(255) NOT NULL (CSV)
  - `consentement_partage_global` boolean NOT NULL
  - `occupation_bien_par_un_indivisaire` boolean NOT NULL
  - `indivision_duree_annees` integer NOT NULL
  - `demande_mesures_conservatoires` boolean NOT NULL
  - `conflit_ouvert_entre_indivisaires` boolean NOT NULL
  - `country` varchar(20) NOT NULL
  - `result_data` text NOT NULL (sérialisation `IndivisionResult`)
  - `created_at`, `updated_at` timestamptz NOT NULL

### Visibility rule
- UUID `f1a04001-0000-0000-0000-ee00000fa221`
- Layer ALWAYS_ON, FRANCE + DROIT_FAMILLE
- Tool id `F-FA-22-indivision`
- Priority `80`

### Composants Java
- `IndivisionCalculator` (algorithme art. 815 Cciv + 1364 CPC)
- `IndivisionRequest` / `IndivisionResponse` / `IndivisionResult` (records)
- `IndivisionAnalysis` (entity)
- `IndivisionRepository` (Spring Data)
- `IndivisionService` (orchestration + persistance + gates)
- `IndivisionController` (POST + GET)

## Hors périmètre

- **Frontend** (SF-FA-22-02 livrée en parallèle avec contrat figé)
- Régime BE équivalent (loi 13 août 2011 — art. 1207 et s. C. jud.) — SF jumelle si retenu après V8
- Génération d'actes de partage (procès-verbal, état liquidatif notarial) — voir F-FA-04
- Calcul détaillé des récompenses (voir F-FA-15)
- Mesures conservatoires détaillées (saisie conservatoire, référé) — voir F-FA-23
- Successions et indivisions successorales — voir F-FA-24

## Analyse de cohérence transversale

- **Auth / Principal** : aucune modification — réutilise `OidcUser + Principal + CurrentUserResolver` (pattern F-FA-12/15/19)
- **Workspace context** : aucune modification — gate `workspace.country == "FRANCE"` + `caseFile.legalDomain == "DROIT_FAMILLE"`, aligné sur F-FA-12/13/15/19
- **Plans / limites** : aucun gate plan — outil ALWAYS_ON
- **Navigation / routing** : aucune (frontend SF-FA-22-02)
- **Outil décisionnel métier** : nouvel outil. Scan effectué sur les outils décisionnels famille existants (F-FA-08/09/10/11/12/13/14/15/19) — chacun couvre une situation distincte (divorces, mesures provisoires, ordonnance protection, récompenses, autorité parentale). L'indivision post-communautaire est un sujet **distinct** non couvert : aucun chevauchement, classement = nouvel outil dédié, pas de scission ni d'extension.

## Nouveau pattern UI ou service partagé

Aucun composant partagé / DTO réutilisable / endpoint transversal introduit. Contrat strictement scoped à `/api/v1/case-files/{id}/indivision`. Réutilisation des patterns existants (records DTO, entity 1:1 par dossier, service `@Transactional`, calculator pur, visibility rule ALWAYS_ON).

## Impact par domaine métier

- **DROIT_DU_TRAVAIL** : non applicable (sujet famille — patrimoine commun).
- **DROIT_FAMILLE** : cœur de la SF.
- **IMMIGRATION** : non applicable.
- **FRANCE** : couvert (art. 815 Cciv + 1364 CPC).
- **BELGIQUE** : non couvert dans cette SF (gate strict 400). Loi 13 août 2011 BE applicable — à proposer comme SF jumelle si V8 confirme l'intérêt produit.

## Parité des domaines métier

Cet outil est de **niveau 5** (scoring d'éligibilité au partage judiciaire). Vérification de l'équivalence sur les 2 autres domaines :
- **DROIT_DU_TRAVAIL** : concept non transposable (indivision = sujet patrimonial famille).
- **IMMIGRATION** : concept non transposable.

L'outil est **intrinsèquement domain-specific** ; aucune feature jumelle requise. Côté BE-FR (parité géographique), la version BE est explicitement repoussée à une SF jumelle ultérieure si la demande produit le confirme.

## Contrat API (figé pour SF-FA-22-02 frontend)

### POST `/api/v1/case-files/{caseFileId}/indivision`

Request :
```json
{
  "dateOrigineIndivision": "2023-04-15",
  "natureBiens": ["IMMOBILIER"],
  "valeurEstimeeTotaleEur": 250000.00,
  "nbIndivisaires": 2,
  "quotesPart": [50.0, 50.0],
  "tentativesPartageAmiable": ["MEDIATION"],
  "consentementPartageGlobal": false,
  "occupationBienParUnIndivisaire": true,
  "indivisionDuréeAnnees": 3,
  "demandeMesuresConservatoires": false,
  "conflitOuvertEntreIndivisaires": true
}
```

Enums :
- `natureBiens` (array, ≥1) : `IMMOBILIER`, `MOBILIER`, `COMPTES_BANCAIRES`, `TITRES_FINANCIERS`, `FONDS_COMMERCE`, `AUTRE`
- `tentativesPartageAmiable` (array) : `PROPOSITION_NOTAIRE`, `MEDIATION`, `EXPERTISE_VALORISATION`, `LICITATION_AMIABLE`, `AUCUNE`

Response 200 :
```json
{
  "caseFileId": "uuid",
  "dateOrigineIndivision": "2023-04-15",
  "natureBiens": ["IMMOBILIER"],
  "valeurEstimeeTotaleEur": 250000.00,
  "nbIndivisaires": 2,
  "quotesPart": [50.0, 50.0],
  "tentativesPartageAmiable": ["MEDIATION"],
  "consentementPartageGlobal": false,
  "occupationBienParUnIndivisaire": true,
  "indivisionDureeAnnees": 3,
  "demandeMesuresConservatoires": false,
  "conflitOuvertEntreIndivisaires": true,
  "scoreEligibilitePartageJudiciaire": 80,
  "verdictRecommandation": "PARTAGE_JUDICIAIRE_RECOMMANDE",
  "indemniteOccupationDueEur": 15000.00,
  "expertiseNotarialeRecommandee": true,
  "licitationRecommandee": true,
  "delaiProcedurePartageJudiciaireMois": 18,
  "baseJuridique": "Art. 815 Cciv + art. 1364 CPC",
  "formule": "Score 80/100 = ...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

`verdictRecommandation` enum : `PARTAGE_AMIABLE_POSSIBLE`, `PARTAGE_JUDICIAIRE_RECOMMANDE`, `LICITATION_REQUISE`, `MEDIATION_PREALABLE`

Codes erreur :
- `400` : validation (dates invalides, enums, quotesPart, valeurs négatives, dossier BE, dossier DT, payload manquant)
- `404` : dossier inexistant ou cross-workspace
