# Mini-spec — F-214 / SF-214-17 — Demande OFPRA introduction (GUDA/ADA) — backend

## Identifiant

`F-214 / SF-214-17`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Guider l'avocat dans la procédure d'introduction de la demande d'asile à l'OFPRA via le GUDA (guichet unique demande d'asile) et l'ADA (allocation demandeur d'asile), avec les délais d'introduction et les documents à préparer.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/ofpra-introduction-analysis`
- Body : `dateArriveeEnFrance` (LocalDate, requis), `passageGudaEffectue` (boolean), `datePassageGuda` (LocalDate, optionnel), `adaRequise` (boolean)
- Calculator `OfpraIntroductionCalculator` :
  - `dateEcheanceIntroduction` = dateArriveeEnFrance + 90 j (délai réglementaire pour déposer à l'OFPRA depuis le passage GUDA)
  - `statutDelai` ∈ {`A_DEPOSER`, `URGENT` (< 21 j), `EXPIRE`}
  - `etapesAPrendre` : liste ordonnée (1. GUDA, 2. Récépissé, 3. ADA si requise, 4. Préparation dossier OFPRA, 5. Dépôt)
  - `piecesRequises` : liste minimale (formulaire OFPRA, état civil, photos, exposé persécutions)
  - `procedureAccelerееRisque` : boolean — si pays d'origine en liste de pays sûrs → risque procédure accélérée L. 531-24 (délai réduit)
- Output persisté dans `ofpra_introduction_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/ofpra-introduction-analysis` → 200 ou 404

---

## Source juridique

- **L. 521-1 à L. 521-15 CESEDA** — demande d'asile, enregistrement.
- **L. 521-7 CESEDA** — ADA (allocation demandeur d'asile).
- **R. 521-1 à R. 521-14 CESEDA** — procédure GUDA, délai 90 j.
- **L. 531-24 à L. 531-31 CESEDA** — procédure accélérée (pays sûrs, fraude).
- **CE 5 juillet 2013, n° 370099** — conditions d'enregistrement.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dateArriveeEnFrance` | date | `aesDateEntreeFrance` (proxy) | Réutiliser |
| `passageGudaEffectue` | boolean | Absent | Extension record + prompt (`gudaPassageEffectue`) |

**Trigger CONTEXTUAL** : `procedureAsileDetectee` (existant F-201). L'outil apparaît si `procedure_asile_detectee = true`.

---

## Critères d'acceptation

- [x] POST statut A_DEPOSER retourne etapesAPrendre, piecesRequises
- [x] POST procedureAccelerereRisque = true si pays en liste pays sûrs (seeder liste initiale)
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-33-ofpra-introduction-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`procedure_asile_detectee`

## Plan de test minimal

- **UT** `OfpraIntroductionCalculatorTest` : 6+ cas (délais, pays sûrs, étapes)
- **IT** `OfpraIntroductionControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `ofpra_introduction_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : champ `gudaPassageEffectue` (boolean)
- **Endpoint** `OfpraIntroductionController`

## Hors périmètre

- Composant Angular (SF-214-18)
- Procédure CNDA (F-IM-12 existant pour Dublin/accélérée)
