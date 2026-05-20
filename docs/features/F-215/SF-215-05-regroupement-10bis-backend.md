# SF-215-05 — Regroupement art. 10bis ressortissant tiers séjour limité — backend

## Identifiant
`F-215 / SF-215-05`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-05-regroupement-10bis-be-backend`

---

## Objectif
Livrer le Calculator + Service + Entity + Endpoint backend pour l'outil `F-IM-27-regroupement-10bis-be` : analyse d'éligibilité au regroupement familial art. 10bis (ressortissant tiers en séjour limité — carte A), avec seuil ressources identique à 10ter, BELGIQUE UNIQUEMENT. Cette SF crée le nouveau flag IA `regroupement_10bis_detecte`.

---

## Comportement attendu

### Cas nominal
- POST `/api/v1/case-files/{caseFileId}/regroupement-10bis-be-analysis`
- Body identique à SF-215-03 sauf `typeCarteRegroupant` : enum `CARTE_A` uniquement (séjour limité)
- `Regroupement10bisBeCalculator` : logique identique à `Regroupement10terBeCalculator` avec :
  - `conditionDuree` = dureeSejour ≥ 12 mois ET titre A en cours de validité
  - `conditionTitreEnCours` = dateFinCarteA ≥ today (champ supplémentaire requis)
  - Même seuil ressources 1 500 €
  - Mêmes pondérations scoring 0-100
- GET `/api/v1/case-files/{caseFileId}/regroupement-10bis-be-analysis`

### Différences clés 10bis vs 10ter

| Critère | 10bis (SF-215-05) | 10ter (SF-215-03) |
|---------|-------------------|-------------------|
| Type carte regroupant | Carte A (limité) | Carte B ou C (illimité) |
| Condition durée | 12 mois carte A valide | 12 mois séjour légal ininterrompu |
| Champ supplémentaire | `dateFinCarteA` (LocalDate) | Aucun |
| Condition titre | Titre A non expiré | Titre illimité = permanent |

### Extension prompt IA — nouveau flag `regroupement_10bis_detecte`

```java
// SF-215-05 — NOUVEAU FLAG — BELGIQUE UNIQUEMENT
Boolean regroupementTiersLimiteDetecte  // = regroupement_10bis_detecte
```

**Indices textuels** : « 10bis », « carte A », « séjour limité », « regroupement conjoint porteur carte A », « art. 10bis Loi 15/12/1980 ».

Extension identique à SF-215-03 (4 champs pré-fill spécifiques 10bis + 1 flag).

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| dateFinCarteA dans le passé | carteA expirée — conditionTitre = false (pas une erreur 400 — verdict INELIGIBLE) | 200 |
| revenusMensuels hors bornes | 400 | 400 |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| SF-215-03 (`Regroupement10terBeCalculator`) | Oui | Logique partagée — même base de scoring. Distinguer par classe distincte (un outil = une situation). |
| Seuil ressources 1 500 € | Oui | Constante partagée `RegroupementBeConstants.SEUIL_RESSOURCES_MENSUEL` |

---

## Conformité F-IA-04
- [ ] **Non applicable** — SF backend pure. Composant Angular : SF-215-06.

---

## Champs IA à extraire (pré-remplissage)

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|-------|------|-----------------------------------------|-----------|
| `lienFamilial` | enum | `be10bisLienFamilial` | Nouveau |
| `revenusMensuelsNetsRegroupant` | entier | `be10bisRevenusMensuels` | Nouveau |
| `dureeSejour` | entier | `be10bisDureeSejour` | Nouveau |
| `dateFinCarteA` | date | `be10bisDateFinCarteA` | Nouveau |

4 champs dans `ImmigrationExtractedData` + prompt `IMMIGRATION_INSTRUCTION`.

---

## Critères d'acceptation

- [ ] POST nominal → 200 avec verdict + différentiel ressources + critères non remplis
- [ ] POST workspace FR → 400
- [ ] `dateFinCarteA` dans le passé → verdict INELIGIBLE + `conditionTitreEnCours = false`
- [ ] Flag `regroupement_10bis_detecte` propagé dans `extractDetectedSituations`
- [ ] UT Calculator : ≥ 8 cas
- [ ] IT Controller : ≥ 6 tests
- [ ] `F-IM-27-regroupement-10bis-be` dans `KNOWN_FRONTEND_TOOL_IDS`
- [ ] Migration : table `regroupement_10bis_be_analyses` + visibility rule CONTEXTUAL

---

## Hors périmètre
- Composant Angular (SF-215-06)
- Regroupement 10ter (SF-215-03/04)

---

## Plan de test

### Tests unitaires
- `Regroupement10bisBeCalculatorTest` : ELIGIBLE, SOUS_RESERVE, INELIGIBLE, carte A expirée, ressources insuffisantes, durée courte

### Tests d'intégration
- `Regroupement10bisBeControllerIT`

---

## Analyse d'impact
- [x] Outil décisionnel métier — `F-IM-27-regroupement-10bis-be`

---

## Notes et décisions
- Source : Loi 15/12/1980 art. 10bis ; AR 17/05/2007 ; AR 11/06/2018.
- Constante `RegroupementBeConstants.SEUIL_RESSOURCES_MENSUEL = 1500` partagée entre SF-215-03 et SF-215-05.
