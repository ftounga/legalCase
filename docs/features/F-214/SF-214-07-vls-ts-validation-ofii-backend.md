# Mini-spec — F-214 / SF-214-07 — Validation VLS-TS OFII 3 mois — backend

## Identifiant

`F-214 / SF-214-07`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-214-07-vls-ts-ofii-backend`

---

## Objectif

Calculer les délais critiques de validation du VLS-TS (visa long séjour valant titre de séjour) auprès de l'OFII, notamment le délai de 3 mois après l'entrée en France, et alerter sur les risques d'irrégularité si la validation n'est pas effectuée dans ce délai.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/vls-ts-validation-analysis`
- Body : `dateEntreeFrance` (LocalDate, requis), `typeVlsTs` (enum `ETUDIANT` | `SALARIE` | `VISITEUR` | `CONJOINT_FRANCAIS` | `AUTRE`), `validationOFIIEffectuee` (boolean), `dateValidationOFII` (LocalDate, optionnel)
- Calculator `VlsTsValidationCalculator` :
  - `dateEcheanceValidation` = dateEntreeFrance + 3 mois (R. 311-3 CESEDA)
  - `joursRestantsValidation` = dateEcheanceValidation - today (négatif si dépassé)
  - `statut` ∈ {`A_VALIDER`, `URGENT` (≤ 15 j), `EXPIRE` (délai dépassé + non validé), `VALIDE` (validation effectuée)}
  - `risqueIrregularite` : boolean — si statut EXPIRE → true
  - `procedureRecours` : si EXPIRE et validationOFIIEffectuee = false → texte de recours pour faute de l'administration (ANEF indisponible, etc.)
- Output persisté dans `vls_ts_validation_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/vls-ts-validation-analysis` → 200 ou 404

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_IMMIGRATION | 400 |
| dateEntreeFrance future | 400 |
| dateEntreeFrance > 24 mois dans le passé (VLS-TS expiré depuis longtemps → hors périmètre) | 400 |
| dateValidationOFII avant dateEntreeFrance | 400 |
| caseFile inaccessible | 404 |

---

## Source juridique

- **R. 311-3 CESEDA** — obligation de validation du VLS-TS dans les 3 mois de l'entrée en France.
- **R. 311-13 CESEDA** — validité du VLS-TS comme titre de séjour.
- **Arrêté du 27/04/2021** (ANEF) — modalités dématérialisées de validation.
- **CE 16 juillet 2014, n° 375479** — responsabilité de l'administration en cas d'impossibilité de valider (analogie panne ANEF).
- **Circulaire INTV1518773C du 12/10/2015** (à vérifier) — instructions aux préfectures sur la validation VLS-TS et les délais.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dateEntreeFrance` | date | `aesDateEntreeFrance` | Déjà présent (SF-246-18) — réutiliser |
| `typeVlsTs` | enum | `typeTitreSejourCode` (proxy) | Dériver depuis `inferredChecklistType` |
| `validationOFIIEffectuee` | boolean | Absent | Extension record + prompt (`vlsTsValidationOFIIEffectuee`) |

**ALWAYS_ON** : cet outil est ALWAYS_ON car le délai de 3 mois est irréversible et s'applique à tout dossier où un VLS-TS a été délivré. La détection du VLS-TS dans `typeTitreSejourCode` suffit à le rendre visible (trigger existant F-IM-01 `inferredChecklistType`).

---

## Critères d'acceptation

- [x] POST statut A_VALIDER retourne 200 avec dateEcheanceValidation, joursRestantsValidation, risqueIrregularite=false
- [x] POST statut EXPIRE retourne risqueIrregularite=true + procedureRecours non null
- [x] POST statut URGENT retourne 200 avec joursRestantsValidation ≤ 15
- [x] POST statut VALIDE (validationOFIIEffectuee=true) retourne joursRestantsValidation null + statut VALIDE
- [x] POST dateEntreeFrance future → 400
- [x] POST dateValidationOFII avant dateEntreeFrance → 400
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] POST upsert → remplacement
- [x] Isolation workspace
- [x] `F-IM-28-vls-ts-validation-ofii-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed `decision_tool_visibility_rules` : ALWAYS_ON, DROIT_IMMIGRATION, FRANCE

## Plan de test minimal

- **UT** `VlsTsValidationCalculatorTest` : 8+ cas (A_VALIDER, URGENT, EXPIRE, VALIDE, dates, Clock fixé)
- **IT** `VlsTsValidationControllerIT` : 6+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `vls_ts_validation_analyses`
- **Migration Liquibase** + seed visibility rules (ALWAYS_ON)
- **Extension** `ImmigrationExtractedData` + prompt : champ `vlsTsValidationOFIIEffectuee`
- **Endpoint** `VlsTsValidationController`

## Hors périmètre

- Composant Angular (SF-214-08)
- Renouvellement du titre post-VLS-TS (F-IM-01 checklist existante)
