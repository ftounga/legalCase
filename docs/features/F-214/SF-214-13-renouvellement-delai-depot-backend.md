# Mini-spec — F-214 / SF-214-13 — Renouvellement délai dépôt 2 mois avant — backend

## Identifiant

`F-214 / SF-214-13`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Calculer le délai optimal de dépôt du renouvellement du titre de séjour (2 mois avant expiration, R. 433-1 CESEDA) et alerter sur le risque d'interruption des droits en cas de dépôt tardif.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/renouvellement-delai-analysis`
- Body : `dateExpirationTitre` (LocalDate, requis), `dateDepotDossier` (LocalDate, optionnel), `typeTitre` (string, optionnel)
- Calculator `RenouvellementDelaiCalculator` :
  - `dateOptimalDepot` = dateExpirationTitre - 2 mois
  - `dateDepotImperatif` = dateExpirationTitre - 1 mois (seuil critique)
  - `joursRestantsAvantOptimal` = dateOptimalDepot - today
  - `joursRestantsAvantImperatif` = dateDepotImperatif - today
  - `statut` ∈ {`A_DEPOSER_URGENT` (< 30 j), `A_DEPOSER` (< 60 j), `EN_AVANCE` (> 60 j), `DEPOSE` (dateDepotDossier renseigné), `EXPIRE` (titre expiré)}
  - Si `dateDepotDossier` renseigné : vérification que le dépôt est dans les 2 mois pré-expiration
  - `risqueIrruption` : boolean — si titre expiré et pas de dépôt → irrégularité
- Output persisté dans `renouvellement_delai_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/renouvellement-delai-analysis` → 200 ou 404

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_IMMIGRATION | 400 |
| dateExpirationTitre absente | 400 |
| dateDepotDossier > dateExpirationTitre + 15 j (dépôt hors délai mais noté pour info) | Accepté avec `alerteRetard=true` |
| caseFile inaccessible | 404 |

---

## Source juridique

- **R. 433-1 CESEDA** — délai de dépôt du renouvellement (2 mois avant expiration).
- **R. 433-2 CESEDA** — effet du récépissé délivré en cas de dépôt dans les délais (maintien du séjour régulier).
- **Jurisprudence TA** (diverses) — conséquences du dépôt tardif (pas de récépissé → titre invalide).
- **CE 25 octobre 2004, n° 258806** (à vérifier) — obligation de dépôt dans les délais.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dateExpirationTitre` | date | `dateExpirationTitre` | Déjà présent — réutiliser |
| `typeTitre` | texte | `typeTitreSejour` | Déjà présent — réutiliser |

**ALWAYS_ON** : outil ALWAYS_ON — tout dossier Immigration FR avec un titre a une date d'expiration. Pas de flag CONTEXTUAL requis. La date d'expiration est déjà extraite par le pipeline IA.

---

## Critères d'acceptation

- [x] POST statut A_DEPOSER retourne joursRestantsAvantOptimal, dateOptimalDepot
- [x] POST statut EXPIRE retourne risqueIrruption=true
- [x] POST dateExpirationTitre absente → 400
- [x] POST workspace BE → 400
- [x] POST avec dateDepotDossier tardif → 200 + alerteRetard=true
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-31-renouvellement-delai-depot-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed `decision_tool_visibility_rules` : ALWAYS_ON, DROIT_IMMIGRATION, FRANCE

## Plan de test minimal

- **UT** `RenouvellementDelaiCalculatorTest` : 8+ cas (Clock fixé, statuts, dépôt tardif)
- **IT** `RenouvellementDelaiControllerIT` : 6+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `renouvellement_delai_analyses`
- **Migration Liquibase** + seed visibility rules (ALWAYS_ON)
- **Endpoint** `RenouvellementDelaiController`
- Pas d'extension `ImmigrationExtractedData` (`dateExpirationTitre` déjà présent)

## Hors périmètre

- Composant Angular (SF-214-14)
- Procédure de renouvellement détaillée (F-IM-01 + F-IM-21 existants)
