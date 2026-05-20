# Mini-spec — F-214 / SF-214-11 — AES calcul présence prouvée — backend

## Identifiant

`F-214 / SF-214-11`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-214-11-aes-presence-prouvee-backend`

---

## Objectif

Calculer le nombre d'années de présence effective en France avec preuves documentaires acceptables (circulaire Valls 28/11/2012) — outil transversal aux 4 voies AES (famille 5 ans, humanitaire 10 ans, étudiant 3 ans, métiers tension 3 ans — L. 435-1+ et L. 435-3).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/aes-presence-prouvee-analysis`
- Body : `periodesPresentees` (liste de `{debut: LocalDate, fin: LocalDate, typePiece: enum AES_PIECE_TYPE}`)
  - `AES_PIECE_TYPE` : `RIB_BANQUE` | `FACTURE_EDF_GAZ` | `QUITTANCE_LOYER` | `BULLETIN_SALAIRE` | `AVIS_IMPOSITION` | `SCOLARITE_ENFANT` | `ATTESTATION_EMPLOYEUR` | `TITRE_SEJOUR` | `AUTRE`
- Calculator `AesPresenceProuveeCalculator` :
  - Fusionne les périodes chevauchantes
  - Calcule durée totale en mois / années
  - Vérifie admissibilité des pièces par voie AES (circulaire Valls : liste des pièces acceptées)
  - `anneesTotalesProuvees` : durée totale en années complètes
  - `eligibiliteParVoie` : objet `{aes_famille: boolean, aes_humanitaire: boolean, aes_etudiant: boolean, aes_metiers_tension: boolean}` avec seuils (5/10/3/3 ans)
  - `gapsPeriodes` : liste de lacunes (périodes sans preuve)
  - `recommandationsPieces` : suggestions de pièces pour combler les gaps
- Output persisté dans `aes_presence_prouvee_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/aes-presence-prouvee-analysis` → 200 ou 404

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_IMMIGRATION | 400 |
| periodesPresentees vide | 400 |
| periode.fin avant periode.debut | 400 |
| periode.debut future | 400 |
| caseFile inaccessible | 404 |

---

## Source juridique

- **Circulaire du 28/11/2012 (Valls)** — liste des pièces acceptées pour justifier la présence habituelle en France. Pièces admises : factures EDF/GDF, quittances loyer, RIB, bulletins de salaire, avis d'imposition, certificats de scolarité enfants.
- **L. 435-1 CESEDA** — AES motif familial (5 ans) et humanitaire (10 ans).
- **L. 435-3 CESEDA** (loi 2024) — AES métiers en tension (3 ans).
- **CE 4 décembre 2009, n° 310980** — présence habituelle, notion d'ancienneté.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `aesDureePresenceMois` (global) | int | `aesDureePresenceMois` | Déjà présent (SF-246-18) — réutiliser pour valeur initiale |
| `aesDateEntreeFrance` | date | `aesDateEntreeFrance` | Déjà présent (SF-246-18) |

Les périodes détaillées avec pièces ne peuvent pas être extraites automatiquement → saisie manuelle. Le champ IA initialise la durée totale estimée.

**Trigger CONTEXTUAL** : l'outil apparaît si au moins un des 4 flags AES existants est true (F-201 : `aesMetiersTensionEligibleDetecte` OR `aesFamilialEligibleDetecte` OR `aesHumanitaireEligibleDetecte` OR `aesEtudiantEligibleDetecte`). Utiliser une règle OR dans `DecisionToolVisibilityService` ou ajouter un flag dérivé `aesCalculPresenceDeclenche`. **Recommandation** : ajouter le flag dérivé dans `ImmigrationExtractedData` pour simplifier la règle de visibilité.

---

## Critères d'acceptation

- [x] POST nominal retourne eligibiliteParVoie, anneesTotalesProuvees, gapsPeriodes, recommandationsPieces
- [x] POST AES famille eligible (5 ans prouvés) → eligibiliteParVoie.aes_famille=true
- [x] POST humanitaire inéligible (9 ans) → eligibiliteParVoie.aes_humanitaire=false
- [x] POST periodesFin avant debut → 400
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] POST upsert → remplacement
- [x] Isolation workspace
- [x] `F-IM-30-aes-presence-prouvee-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed `decision_tool_visibility_rules` : CONTEXTUAL, trigger_field=`aes_calcul_presence_declenche`

## Plan de test minimal

- **UT** `AesPresenceProuveeCalculatorTest` : 8+ cas (fusion périodes, gaps, seuils, 4 voies)
- **IT** `AesPresenceProuveeControllerIT` : 6+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `aes_presence_prouvee_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : flag `aesCalculPresenceDeclenche` (boolean, dérivé : true si l'un des 4 flags AES est true) + prompt update minimal
- **Endpoint** `AesPresenceProuveeController`

## Hors périmètre

- Composant Angular (SF-214-12)
- Les 4 outils AES eux-mêmes (F-IM-09, existants)
