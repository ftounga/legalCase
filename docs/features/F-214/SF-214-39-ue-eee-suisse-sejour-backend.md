# Mini-spec — F-214 / SF-214-39 — UE/EEE/Suisse droit au séjour — backend

## Identifiant

`F-214 / SF-214-39`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Analyser le droit au séjour automatique des citoyens UE/EEE/Suisse en France et des membres de leur famille (carte « membre famille citoyen UE »), régime totalement distinct du CESEDA.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/ue-eee-suisse-sejour-analysis`
- Body : `nationalite` (string), `estCitoyenUE` (boolean), `membreFamilleNonUE` (boolean, si family member d'un citoyen UE), `dureeSejourMois` (int), `activiteProfessionnelle` (enum `SALARIE` | `INDEPENDANT` | `ETUDIANT` | `RETRAITE` | `SANS_ACTIVITE_RESSOURCES_SUFFISANTES`)
- Analyzer `UeEeeSuisseSejourAnalyzer` :
  - `droitSejourAutomatique3Mois` : toujours true pour citoyen UE
  - `droitSejourPlus5Ans` : si dureeSejourMois ≥ 60 + conditions activité/ressources → droit permanent (Directive 2004/38 art. 16)
  - `titreObtenu` : `ATTESTATION_ENREGISTREMENT` (demande facultative, délivrée en préfecture) ou `CARTE_SEJOUR_MEMBRE_FAMILLE` (non UE obligatoire)
  - `conditionsRespectees` : liste critères selon activité
  - `situationMenbreNonUE` : si membreFamilleNonUE → carte « membre famille citoyen UE » (Directive 2004/38 art. 10), obligatoire
  - `baseJuridique` : Directive 2004/38 CE + L. 233-1+ CESEDA
- Output persisté dans `ue_eee_suisse_sejour_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/ue-eee-suisse-sejour-analysis` → 200 ou 404

---

## Source juridique

- **Directive 2004/38/CE** du 29/04/2004 — droit des citoyens UE de séjourner librement.
- **L. 233-1 à L. 234-12 CESEDA** — transposition directive (anciens L. 121-1+).
- **R. 233-1 à R. 234-10 CESEDA** — attestation d'enregistrement, carte membre famille.
- **CE 6 novembre 2015, n° 385654** — conditions droit permanent art. 16.
- **CJUE 21 juillet 2011, C-325/09 Dias** — interruption séjour et droit permanent.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `nationalite` | texte | `nationalite` | Déjà présent (F-235) — réutiliser |
| `estCitoyenUE` | boolean | `nationaliteUe` | Déjà présent — réutiliser |
| `dureeSejourMois` | int | `aesDureePresenceMois` | Réutiliser |

**Trigger CONTEXTUAL** : `nationaliteUe = true` (champ booléen existant dans `ImmigrationExtractedData`). Pas de nouveau flag requis.

---

## Critères d'acceptation

- [x] POST citoyen UE < 5 ans → droitSejourAutomatique3Mois=true, droitSejourPlus5Ans=false
- [x] POST citoyen UE ≥ 5 ans → droitSejourPlus5Ans=true
- [x] POST membreFamilleNonUE → situationMenbreNonUE + titre CARTE_SEJOUR_MEMBRE_FAMILLE obligatoire
- [x] POST workspace BE → 400 (outil FR-only — en Belgique, régime distinct)
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-44-ue-eee-suisse-sejour-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`nationalite_ue`, trigger_value=`true`

## Plan de test minimal

- **UT** `UeEeeSuisseSejourAnalyzerTest` : 6+ cas
- **IT** `UeEeeSuisseSejourControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `ue_eee_suisse_sejour_analyses`
- **Migration Liquibase** + seed visibility rules (trigger_field=`nationalite_ue`, trigger_value=`true` — pattern F-235 extension booléenne)
- Pas d'extension `ImmigrationExtractedData` (champs existants suffisants)
- **Endpoint** `UeEeeSuisseSejourController`

## Hors périmètre

- Composant Angular (SF-214-40)
- Membre famille UE ressortissant d'un pays tiers en Belgique (F-220 régimes BE)
