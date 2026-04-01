# Mini-spec — F-96 / SF-96-03 : Injection des NON_COMPLIANT dans la re-synthèse enrichie

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-96 / SF-96-03`

## Feature parente

`F-96` — Checklist procédurale interactive

## Statut

`done`

## Date de création

`2026-04-01`

## Branche Git

`feat/SF-96-03-procedure-checks-reanalysis`

---

## Objectif

Injecter les points `NON_COMPLIANT` de la checklist procédurale dans le prompt de re-synthèse enrichie (`EnrichedAnalysisService`), afin que l'IA prenne en compte les non-conformités identifiées par l'avocat lors de la re-analyse.

---

## Comportement attendu

### Cas nominal

1. L'avocat a marqué ≥ 1 point de la checklist comme `NON_COMPLIANT`
2. Il déclenche une re-analyse enrichie (`POST /api/v1/case-files/{id}/re-analyze`)
3. `EnrichedAnalysisService` charge les checks `NON_COMPLIANT` de la dernière analyse DONE du dossier via `ProcedureCheckService.listNonCompliant()`
4. Si des points NON_COMPLIANT existent → une section `[Points procéduraux non conformes]` est injectée dans le prompt utilisateur, après la section `[Échanges libres]` existante
5. La re-synthèse tient compte de ces non-conformités dans ses risques et recommandations

### Cas dégradés

| Situation | Comportement attendu |
|-----------|---------------------|
| Aucun check NON_COMPLIANT | Section absente du prompt — comportement identique à aujourd'hui |
| `listNonCompliant()` lève une exception | Fail-open : re-analyse lancée sans la section, log warn |
| Ancienne analyse sans checks | `listNonCompliant()` retourne `[]` → pas de section |

---

## Critères d'acceptation

- [x] `ProcedureCheckRepository` : `findByCaseAnalysisIdAndStatutOrderByOrdreAsc(UUID, ProcedureCheckStatus)`
- [x] `ProcedureCheckService` : `listNonCompliant(CaseFile)` — charge les NON_COMPLIANT de la dernière analyse DONE, fail-open
- [x] `EnrichedAnalysisService.buildEnrichedPrompt()` : section `[Points procéduraux non conformes]` injectée si liste non vide
- [x] Format : `[Points procéduraux non conformes]\n- <description>\n- ...`
- [x] Aucun check NON_COMPLIANT → prompt identique à aujourd'hui (pas de régression)
- [x] Exception dans `listNonCompliant()` → fail-open, warn loggué, re-analyse lancée normalement

---

## Plan de test

### Tests unitaires

- [x] TC-01 : `buildEnrichedPrompt` avec checks NON_COMPLIANT → section injectée
- [x] TC-02 : `buildEnrichedPrompt` sans checks NON_COMPLIANT → section absente
- [x] TC-03 : exception dans `listNonCompliant()` → fail-open, `prepareEnrichedAnalysis` retourne un résultat sans la section

### Tests d'intégration

- [x] Non requis — pas de nouvel endpoint

### Isolation workspace

- [x] Garantie par `listNonCompliant(CaseFile)` qui délègue à `caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc` — même isolation que les autres méthodes

---

## Périmètre

### Hors scope (explicite)

- Modification du frontend
- Modification du prompt de l'analyse STANDARD
- Réinitialisation des checks NON_COMPLIANT après re-analyse

---

## Technique

### Fichiers modifiés

| Fichier | Modification |
|---------|-------------|
| `ProcedureCheckRepository.java` | `findByCaseAnalysisIdAndStatutOrderByOrdreAsc()` |
| `ProcedureCheckService.java` | `listNonCompliant(CaseFile)` fail-open |
| `EnrichedAnalysisService.java` | `buildEnrichedPrompt()` + 4ème param + injection section |
| `EnrichedAnalysisServiceTest.java` | 3 nouveaux tests TC-01/02/03 + mise à jour des appels existants |

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification interne du pipeline IA

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- SF-96-01 mergée ✅
- SF-96-02 mergée ✅

### Questions ouvertes

- Aucune
