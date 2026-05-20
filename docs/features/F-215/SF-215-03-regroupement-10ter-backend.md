# SF-215-03 — Regroupement art. 10ter ressortissant tiers séjour illimité — backend

## Identifiant
`F-215 / SF-215-03`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-03-regroupement-10ter-be-backend`

---

## Objectif
Livrer le Calculator + Service + Entity + Endpoint backend pour l'outil `F-IM-26-regroupement-10ter-be` : analyse d'éligibilité au regroupement familial art. 10ter (ressortissant tiers en séjour illimité — carte B ou C), avec vérification du seuil de ressources (≈ 1 500 €/mois soit 120 % du revenu d'intégration sociale × 1,5), BELGIQUE UNIQUEMENT. Cette SF crée également le nouveau flag IA `regroupement_10ter_detecte`.

---

## Comportement attendu

### Cas nominal
- POST `/api/v1/case-files/{caseFileId}/regroupement-10ter-be-analysis`
- Body : `lienFamilial` (enum : CONJOINT / PARTENAIRE_ENREGISTRE / ENFANT_MOINS_21 / ENFANT_21_PLUS_CHARGE / ASCENDANT_CHARGE, requis), `typeCarteRegroupant` (enum : CARTE_B / CARTE_C, requis), `revenusMensuelsNetsRegroupant` (Integer, requis), `dureeSejour` (Integer — nombre de mois de séjour légal ininterrompu en BE, requis), `logementConforme` (Boolean, requis), `assuranceMaladie` (Boolean, requis), `menaceOrdrePublic` (Boolean, requis)
- `Regroupement10terBeCalculator` calcule :
  - `seuilRessources` = 1 500 (montant paramétrable — 120 % × RIS mensuel × 1,5 = ~1 495 €, AR 17/05/2007, à vérifier par avocat BE)
  - `conditionRessources` = revenusMensuelsNetsRegroupant ≥ seuilRessources
  - `conditionDuree` = dureeSejour ≥ 12 mois (séjour ininterrompu requis, art. 10ter)
  - `conditionLogement` = logementConforme
  - `conditionAssurance` = assuranceMaladie
  - `conditionOrdrePublic` = !menaceOrdrePublic
  - `scoreEligibilite` (0-100, pondéré : ressources 40, durée 20, logement 20, assurance 10, ordre public 10)
  - `verdict` ∈ { ELIGIBLE (score ≥ 80), SOUS_RESERVE (score 50-79), INELIGIBLE (score < 50) }
  - `criteresNonRemplis` : liste des conditions en échec
  - `differentielRevenus` = revenusMensuelsNetsRegroupant − seuilRessources (signé — utile pour contester le calcul OE)
- Output persisté dans `regroupement_10ter_be_analyses` (1:1 par case_file)
- GET `/api/v1/case-files/{caseFileId}/regroupement-10ter-be-analysis` → 200 ou 404

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| workspace.country ≠ BELGIQUE | Outil BE-only | 400 |
| caseFile.legalDomain ≠ DROIT_IMMIGRATION | Mauvais domaine | 400 |
| revenusMensuelsNetsRegroupant < 0 ou > 100 000 | Valeur hors bornes | 400 |
| dureeSejour < 0 ou > 600 | Valeur hors bornes | 400 |
| lienFamilial inconnu | Enum invalide | 400 |
| typeCarteRegroupant inconnu | Enum invalide | 400 |
| caseFile non accessible | Isolation | 404 |

---

## Extension prompt IA — nouveau flag `regroupement_10ter_detecte`

Cette SF crée un **nouveau flag IA** non encore présent dans `ImmigrationExtractedData` ni dans `IMMIGRATION_INSTRUCTION` :

```java
// SF-215-03 — NOUVEAU FLAG — BELGIQUE UNIQUEMENT
Boolean regroupementPourvueTiersIllimite  // = regroupement_10ter_detecte
```

**Indices textuels pour l'extraction** : « 10ter », « carte B », « carte C », « séjour illimité », « regroupement avec ressortissant tiers établi », « art. 10 Loi 15/12/1980 §1 4° », « conjoint porteur carte B/C ».

À ajouter dans :
1. `ImmigrationExtractedData.java` (champ `Boolean regroupementTiersIllimiteDetecte`, en queue des flags BE)
2. `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION` (section « BELGIQUE UNIQUEMENT — Flags regroupement »)
3. `DecisionToolVisibilityService.extractDetectedSituations` (propagation `detected["regroupement_10ter_detecte"] = "true"`)
4. Migration Liquibase : INSERT `decision_tool_visibility_rules` CONTEXTUAL (`regroupement_10ter_detecte=true`, BELGIQUE, DROIT_IMMIGRATION, tool_id=`F-IM-26-regroupement-10ter-be`)

---

## Analyse de cohérence transversale

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `F-IM-14-40ter-familial-belge-be` | Oui | Outil similaire (40ter Belge) — pattern identique à réutiliser. Distinctions clés : 40ter = regroupant Belge ; 10ter = regroupant ressortissant tiers séjour illimité. Deux situations juridiques distinctes (un outil = une situation). |
| SF-215-05 (regroupement 10bis) | Oui | 10bis = regroupant en séjour limité. Même calculator structure mais conditions ressources et durée différentes. 2 outils séparés — invariant respecté. |
| Seuil ressources partagé | Oui | `seuilRessources = 1500` paramétrable dans Calculator — la même constante sera réutilisée par SF-215-05. |
| Autres domaines | Non | Regroupement familial = immigration uniquement. |

### Décision
- SF-215-03 et SF-215-05 coexistent (10ter ≠ 10bis) — invariant "un outil = une situation" respecté.

---

## Conformité F-IA-04
- [ ] **Non applicable** — SF backend pure. Composant Angular : SF-215-04.

---

## Champs IA à extraire (pré-remplissage)

| Champ du formulaire | Type | Champ source `ImmigrationExtractedData` | Extension |
|---------------------|------|-----------------------------------------|-----------|
| `lienFamilial` | enum | `be10terLienFamilial` | Nouveau — whitelist 5 valeurs |
| `typeCarteRegroupant` | enum | `be10terTypeCarte` | Nouveau — whitelist CARTE_B/CARTE_C |
| `revenusMensuelsNetsRegroupant` | entier | `be10terRevenusMensuels` | Nouveau — boundedInt 0-100 000 |
| `dureeSejour` | entier | `be10terDureeSejour` | Nouveau — boundedInt 0-600 |

4 champs IA à ajouter dans `ImmigrationExtractedData` + `IMMIGRATION_INSTRUCTION`.

---

## Critères d'acceptation

- [ ] POST nominal retourne 200 avec `verdict`, `scoreEligibilite`, `criteresNonRemplis`, `differentielRevenus`
- [ ] POST workspace FR → 400
- [ ] POST domaine Travail → 400
- [ ] POST revenus 0 avec `conditionRessources = false`
- [ ] `differentielRevenus` signé négatif quand revenus < seuil
- [ ] Nouveau flag `regroupement_10ter_detecte` propagé dans `extractDetectedSituations`
- [ ] UT Calculator : ≥ 8 cas (ELIGIBLE, SOUS_RESERVE, INELIGIBLE, seuil ressources exact, durée insuffisante, menace OP)
- [ ] IT Controller : ≥ 6 tests
- [ ] `F-IM-26-regroupement-10ter-be` dans `KNOWN_FRONTEND_TOOL_IDS`
- [ ] Migration Liquibase : table + visibility rule CONTEXTUAL

---

## Hors périmètre
- Composant Angular (SF-215-04)
- Regroupement 10bis séjour limité (SF-215-05/06)
- Calcul ressources avec revenus du ménage entier (simplification V1 : revenus du regroupant uniquement)

---

## Tables / endpoints / composants impactés
- Nouvelle table `regroupement_10ter_be_analyses`
- Migration Liquibase `N+1-create-regroupement-10ter-be-analyses.xml`
- Extension `ImmigrationExtractedData` (4 champs + 1 flag) + prompt

---

## Plan de test

### Tests unitaires
- `Regroupement10terBeCalculatorTest` : 8+ cas (ELIGIBLE/SOUS_RESERVE/INELIGIBLE, ressources exactes, durée < 12 mois, menace OP, différentiel signé)

### Tests d'intégration
- `Regroupement10terBeControllerIT` : POST nominal, country guard, domain guard, isolation, GET 404, upsert

### Isolation workspace
- Applicable — test dédié

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Outil décisionnel métier** — nouveau tool_id `F-IM-26-regroupement-10ter-be`

### Smoke tests E2E concernés
- [ ] Aucun — SF backend pure

---

## Dépendances
- F-203 SF-203-01 : `ImmigrationExtractedData` — Done ✅
- Numéro migration disponible (coordination avec SF-215-01)

---

## Notes et décisions
- Seuil 1 500 € : AR 17/05/2007 + révision annuelle. Valeur 2026 à vérifier avec avocat BE. Paramètre `regroupement.seuil-ressources-be=1500` dans `application.properties`.
- Source : Loi 15/12/1980 art. 10 et 10ter ; AR 17/05/2007 conditions ressources/logement/assurance ; AR 11/06/2018.
- Distinction 10ter (séjour illimité) vs 10bis (séjour limité) : conditions ressources identiques mais durée d'ancienneté du regroupant différente — à documenter dans le Calculator.
