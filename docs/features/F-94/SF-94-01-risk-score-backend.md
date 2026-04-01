# Mini-spec — F-94 / SF-94-01 — Score de risque global — Backend

## Identifiant
`F-94 / SF-94-01`

## Feature parente
`F-94` — Score de risque global du dossier

## Statut
`done`

## Date de création
2026-04-01

## Branche Git
`feat/SF-94-01-risk-score-backend`

---

## Objectif

Faire calculer par le LLM un score de risque global (niveau FAIBLE/MOYEN/ELEVE + valeur 0-100) lors de la case analysis (standard et enrichie), le stocker en DB et l'exposer dans l'API.

---

## Comportement attendu

### Cas nominal

1. CaseAnalysisService et EnrichedAnalysisService incluent dans leur system prompt un champ `score_risque: { niveau: "FAIBLE"|"MOYEN"|"ELEVE", valeur: <0-100> }`.
2. CaseAnalysisResponse parse le champ JSON (fail-open : si absent ou malformé → null).
3. La table `case_analyses` reçoit deux nouvelles colonnes nullable : `risk_level VARCHAR(10)` et `risk_score INTEGER`.
4. `populateRiskScore()` persiste `riskLevel` et `riskScore` dans l'entité CaseAnalysis.
5. CaseAnalysisResponse expose `riskLevel` et `riskScore`.
6. CaseFileResponse expose `riskLevel` et `riskScore` (depuis dernière analyse DONE).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| LLM ne retourne pas `score_risque` | null dans les deux champs — pas d'erreur |
| `valeur` hors [0-100] | null — fail-open |
| `niveau` inconnu | null — fail-open |
| Ancienne analyse sans ces champs | null retourné — rétrocompat |

---

## Critères d'acceptation

- [x] System prompt CaseAnalysisService inclut `score_risque`
- [x] System prompt EnrichedAnalysisService idem
- [x] Migration 041 : `risk_level VARCHAR(10) NULL`, `risk_score INTEGER NULL` sur `case_analyses`
- [x] CaseAnalysis entity : champs `riskLevel` et `riskScore` nullable
- [x] `populateRiskScore()` : parse fail-open, validation niveau et plage [0-100]
- [x] CaseAnalysisResponse expose `riskLevel` et `riskScore`
- [x] CaseFileResponse expose `riskLevel` et `riskScore` (depuis dernière analyse DONE)
- [x] Rétrocompat : analyse sans score → null

---

## Périmètre

### Hors scope
- Affichage frontend (SF-94-02)
- Score calculé côté backend sans LLM

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analyses` | ALTER — 2 colonnes nullable | `risk_level`, `risk_score` |

### Migration Liquibase
- [x] `041-add-risk-score-to-case-analyses.xml`

---

## Plan de test

### Tests unitaires

- [x] CaseAnalysisResponse — score présent → riskLevel="MOYEN", riskScore=55
- [x] CaseAnalysisResponse — champ absent → null, null
- [x] CaseAnalysisResponse — niveau inconnu → riskLevel null
- [x] CaseAnalysisResponse — valeur hors [0-100] → riskScore null

### Tests d'intégration

- [x] `populateRiskScore()` appelé dans finalizeCaseAnalysis et finalizeEnrichedAnalysis

### Isolation workspace
- [x] Applicable — inchangée (isolation existante sur case_analyses)
