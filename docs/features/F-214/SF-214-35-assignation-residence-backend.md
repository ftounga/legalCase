# Mini-spec — F-214 / SF-214-35 — Assignation à résidence L. 731-1 — backend

## Identifiant

`F-214 / SF-214-35`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Analyser la validité d'une mesure d'assignation à résidence (alternative à la rétention administrative), calculer la durée maximale et générer les moyens de contestation devant le TA.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/assignation-residence-analysis`
- Body : `dateNotificationAssignation` (LocalDate, requis), `dureeAssignationJours` (int), `motifAssignation` (enum `EXECUTION_OQTF` | `SURVEILLANCE_MESURE_ELOIGNEMENT` | `AUTRE`), `obligationsPresentation` (string, optionnel — fréquence pointage)
- Analyzer `AssignationResidenceAnalyzer` :
  - `dureeTotaleAutoriseeJours` : 45 j maximum (L. 731-1), renouvelable 2 × 45 j = 135 j max
  - `dateEcheanceAssignation` = dateNotificationAssignation + dureeAssignationJours
  - `renouvellementPossible` : boolean
  - `motifsContestation` : liste (défaut de motivation, absence de risque de fuite, proportionnalité, vice de procédure)
  - `recoursPossible` : boolean — recours TA dans les 48 h (L. 732-1 à vérifier)
  - `statut` ∈ {`EN_COURS`, `EXPIRATION_PROCHE` (< 15 j), `EXPIRE`}
- Output persisté dans `assignation_residence_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/assignation-residence-analysis` → 200 ou 404

---

## Source juridique

- **L. 731-1 à L. 733-11 CESEDA** (recodification 2021, anciens L. 561-1+) — assignation à résidence.
- **L. 731-1 CESEDA** — durée 45 j renouvelable.
- **L. 732-1 CESEDA** (à vérifier) — recours TA contre assignation.
- **CE 13 mars 2006, n° 286669** (à vérifier) — conditions de validité assignation.
- **Loi 26/01/2024** : maintien du régime d'assignation à résidence.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `motifAssignation` | enum | `typeProcedureDetectee` (proxy) | Dériver |
| `dateNotificationAssignation` | date | Absent | Extension record + prompt (`assignationDateNotification`) |

**Nouveau flag CONTEXTUAL** : `assignationResidenceDetectee` (boolean) — extraction : mentions "assignation à résidence", "L.731-1", "pointage gendarmerie", "obligation présentation", "assigné à résidence". Ajouté dans `ImmigrationExtractedData` + prompt.

---

## Critères d'acceptation

- [x] POST EN_COURS retourne dateEcheanceAssignation, renouvellementPossible, motifsContestation
- [x] POST durée > 135 j → 400 (dépasse maximum légal)
- [x] POST EXPIRATION_PROCHE → statut + alert
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-42-assignation-residence-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`assignation_residence_detectee`

## Plan de test minimal

- **UT** `AssignationResidenceAnalyzerTest` : 6+ cas
- **IT** `AssignationResidenceControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `assignation_residence_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : flag `assignationResidenceDetectee` + champ `assignationDateNotification`
- **Endpoint** `AssignationResidenceController`

## Hors périmètre

- Composant Angular (SF-214-36)
