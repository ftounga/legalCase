# Mini-spec — F-214 / SF-214-27 — MNA évaluation d'âge + recours — backend

## Identifiant

`F-214 / SF-214-27`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Guider l'avocat dans la procédure d'évaluation d'âge d'un mineur non accompagné (MNA) refusé par l'ASE, incluant les recours devant le juge des enfants (JE) et la contestation des examens osseux.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/mna-evaluation-age-analysis`
- Body : `dateNaissanceDeclaree` (LocalDate, requis), `evaluationASERefusee` (boolean), `dateRefusASE` (LocalDate, optionnel), `examenOsseuxOrdonne` (boolean), `resultatExamenOsseux` (string, optionnel)
- Analyzer `MnaEvaluationAgeAnalyzer` :
  - Vérifie âge déclaré < 18 ans (gate)
  - Si evaluationASERefusee : calcule `dateEcheanceSaisineJE` = dateRefusASE + 5 j (délai urgence)
  - `contestationExamenOsseux` : si examenOsseuxOrdonne → liste des moyens de contestation (fiabilité méthode Greulich-Pyle, marge d'erreur 2 ans, CE jurisprudence)
  - `procedureASE` : étapes (1. Entretien d'évaluation, 2. Refus → saisine JE, 3. Ordonnance provisoire, 4. Placement ASE)
  - `statut` ∈ {`EN_ATTENTE_EVALUATION`, `RECOURS_JE_URGENT`, `EXAMEN_OSSEUX_CONTESTE`, `PRIS_EN_CHARGE`}
  - `droitsAttaches` : liste (hébergement d'urgence, scolarisation, APS L. 425-3)
- Output persisté dans `mna_evaluation_age_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/mna-evaluation-age-analysis` → 200 ou 404

---

## Source juridique

- **Cciv 375** — mesures de protection de l'enfant (ordonnance JE).
- **Cciv 388** — mineur et détermination de l'âge.
- **Arrêté du 17/11/2016** (Cazeneuve) (à vérifier) — protocole d'évaluation MNA.
- **CE 25 juillet 2013, n° 371334** — contestation fiabilité examens osseux.
- **Circulaire Taubira du 31/05/2013** — procédure d'évaluation MNA.
- **L. 425-3 CESEDA** — APS pour MNA pris en charge par ASE > 16 ans.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dateNaissanceDeclaree` | date | `mineursDateNaissance` | Déjà présent (SF-246-19) — réutiliser |
| `evaluationASERefusee` | boolean | Absent | Extension record + prompt (`mnaEvaluationRefusee`) |
| `examenOsseuxOrdonne` | boolean | Absent | Extension record + prompt (`mnaExamenOsseuxOrdonne`) |

**Trigger CONTEXTUAL** : `clientMineurDetecte` (existant F-201).

---

## Critères d'acceptation

- [x] POST dateNaissanceDeclaree ≥ 18 ans → 400 (gate mineur)
- [x] POST RECOURS_JE_URGENT retourne dateEcheanceSaisineJE ≤ 5 j
- [x] POST examenOsseuxOrdonne → contestationExamenOsseux liste
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-38-mna-evaluation-age-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`client_mineur_detecte`

## Plan de test minimal

- **UT** `MnaEvaluationAgeAnalyzerTest` : 6+ cas
- **IT** `MnaEvaluationAgeControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `mna_evaluation_age_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : champs `mnaEvaluationRefusee`, `mnaExamenOsseuxOrdonne`
- **Endpoint** `MnaEvaluationAgeController`

## Hors périmètre

- Composant Angular (SF-214-28)
- Tutelle MNA (P3 → F-220)
