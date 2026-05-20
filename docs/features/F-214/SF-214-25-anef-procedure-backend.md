# Mini-spec — F-214 / SF-214-25 — ANEF procédure / pannes / recours — backend

## Identifiant

`F-214 / SF-214-25`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Guider l'avocat dans les démarches ANEF (administration numérique des étrangers en France) et les recours en cas de panne ou d'impossibilité de dépôt dématérialisé, pour éviter la forclusion du délai de renouvellement.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/anef-procedure-analysis`
- Body : `typeTitreConcerne` (string), `dateExpirationTitre` (LocalDate), `panneeANEFSignalee` (boolean), `dateTentativeDepot` (LocalDate, optionnel), `demandeAdresseePrefecture` (boolean)
- Calculator `AnefProcedureCalculator` :
  - Vérifie si la date d'expiration est proche (< 30 j → URGENT)
  - Si panneeANEFSignalee : génère la procédure de recours pour faute de l'administration (dépôt dossier papier en préfecture, preuve de tentative de connexion, mail recommandé)
  - `etapesAlternatives` : liste (1. Preuve screenshot panne, 2. Envoi LRAR préfecture, 3. Dépôt physique demande, 4. Recours pour faute si préjudice)
  - `delaiRecoursForFaute` : 2 ans (responsabilité administrative)
  - `statut` ∈ {`NORMAL`, `URGENT`, `PANNE_EN_COURS`, `RECOURS_POSSIBLE`}
- Output persisté dans `anef_procedure_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/anef-procedure-analysis` → 200 ou 404

---

## Source juridique

- **R. 311-2-2 CESEDA** (à vérifier) — modalités dématérialisées ANEF.
- **Arrêté du 27/04/2021** — obligations de dématérialisation.
- **CE 16 juillet 2014, n° 375479** (analogie) — responsabilité de l'administration pour faute lors d'impossibilité de dépôt.
- **L. 114-9 CRPA** — délai substitutif en cas d'impossibilité technique.
- **Jurisprudence TA** : plusieurs TA ont admis le dépôt physique en cas de panne ANEF avérée.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dateExpirationTitre` | date | `dateExpirationTitre` | Déjà présent — réutiliser |
| `typeTitreConcerne` | texte | `typeTitreSejour` | Déjà présent |

**Trigger CONTEXTUAL** : `anefPanneDetectee` (nouveau flag) — extraction : mentions "ANEF en panne", "site indisponible", "connexion impossible", "erreur ANEF", "délai dépôt en ligne", "plateforme étrangers". Ajouté dans `ImmigrationExtractedData` + prompt. En l'absence de panne détectée, l'outil n'est pas visible (ne pas polluer tous les dossiers immigration).

---

## Critères d'acceptation

- [x] POST NORMAL retourne statut + étapes standard ANEF
- [x] POST PANNE_EN_COURS retourne etapesAlternatives + delaiRecoursForFaute
- [x] POST URGENT (< 30 j expiration) retourne statut URGENT
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-37-anef-procedure-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`anef_panne_detectee`

## Plan de test minimal

- **UT** `AnefProcedureCalculatorTest` : 5+ cas
- **IT** `AnefProcedureControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `anef_procedure_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : flag `anefPanneDetectee`
- **Endpoint** `AnefProcedureController`

## Hors périmètre

- Composant Angular (SF-214-26)
