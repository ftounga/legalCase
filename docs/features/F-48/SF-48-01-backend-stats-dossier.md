# Mini-spec — F-48 / SF-48-01 — Backend stats dossier

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-48 / SF-48-01`

## Feature parente

`F-48` — Tableau de bord dossier

## Statut

`ready`

## Date de création

2026-03-26

## Branche Git

`feat/SF-48-01-backend-stats-dossier`

---

## Objectif

Exposer un endpoint `GET /api/v1/case-files/{id}/stats` retournant les métriques agrégées d'un dossier : nombre de documents, nombre d'analyses terminées, et total de tokens consommés.

---

## Comportement attendu

### Cas nominal

`GET /api/v1/case-files/{id}/stats` — utilisateur authentifié, membre du workspace propriétaire du dossier.

Réponse `200` :
```json
{
  "documentCount": 3,
  "analysisCount": 2,
  "totalTokens": 12540
}
```

- `documentCount` : nombre de documents non supprimés liés au dossier (`documents` table)
- `analysisCount` : nombre de `case_analyses` avec `analysis_status = 'DONE'` (inclut STANDARD et ENRICHED)
- `totalTokens` : somme de `tokens_input + tokens_output` de tous les `usage_events` du dossier

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Dossier inexistant | `Case file not found` | 404 |
| Dossier appartenant à un autre workspace | `Case file not found` (opaque) | 404 |
| Utilisateur non authentifié | Redirection OAuth | 401 |

---

## Critères d'acceptation

- [ ] `GET /api/v1/case-files/{id}/stats` retourne 200 avec `documentCount`, `analysisCount`, `totalTokens`
- [ ] `documentCount` = nombre réel de documents du dossier
- [ ] `analysisCount` = nombre de `case_analyses` DONE pour ce dossier
- [ ] `totalTokens` = somme `tokens_input + tokens_output` de tous les `usage_events` du dossier
- [ ] Si aucun usage event : `totalTokens = 0`
- [ ] Si aucune analyse : `analysisCount = 0`
- [ ] Retourne 404 si le dossier n'appartient pas au workspace de l'utilisateur courant (isolation)

---

## Périmètre

### Hors scope (explicite)

- Coût estimé en euros
- Durée d'analyse (non persistée)
- Affichage frontend (SF-48-02)
- Statistiques au niveau workspace (déjà dans F-18)

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/stats` | Oui | MEMBER |

### Nouveau DTO

```java
record CaseFileStatsResponse(long documentCount, long analysisCount, long totalTokens) {}
```

### Nouveau service

`CaseFileStatsService` (package `casefile`) :
- Injecte `DocumentRepository`, `CaseAnalysisRepository`, `UsageEventRepository`
- Vérifie que le dossier appartient au workspace courant (même pattern que `CaseFileService.getById`)
- Agrège les 3 métriques en lecture seule (`@Transactional(readOnly = true)`)

### Nouvelles requêtes repository

- `CaseAnalysisRepository` : `countByCaseFileIdAndAnalysisStatus(UUID, AnalysisStatus)` — Spring Data
- `UsageEventRepository` : `@Query` JPQL — `SELECT COALESCE(SUM(u.promptTokens + u.completionTokens), 0) FROM UsageEvent u WHERE u.caseFileId = :caseFileId`

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `documents` | SELECT COUNT | Requête existante |
| `case_analyses` | SELECT COUNT | Nouvelle méthode repository |
| `usage_events` | SELECT SUM | Nouvelle méthode repository |

### Migration Liquibase

- [x] Non applicable — lecture seule, pas de changement de schéma

### Composants Angular

Non applicable (SF-48-01 backend uniquement).

---

## Plan de test

### Tests unitaires

- [ ] `CaseFileStatsService` — cas nominal : retourne les bonnes métriques agrégées
- [ ] `CaseFileStatsService` — dossier inexistant : lève 404
- [ ] `CaseFileStatsService` — workspace différent : lève 404
- [ ] `CaseFileStatsService` — pas d'usage events : `totalTokens = 0`
- [ ] `CaseFileStatsService` — pas d'analyses : `analysisCount = 0`

### Tests d'intégration

- [ ] `GET /api/v1/case-files/{id}/stats` → 200 avec métriques correctes
- [ ] `GET /api/v1/case-files/{id}/stats` → 404 si dossier inexistant
- [ ] `GET /api/v1/case-files/{id}/stats` → 404 si workspace différent (isolation)

### Isolation workspace

- [x] Applicable — test : un utilisateur du workspace A ne peut pas accéder aux stats du dossier du workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — nouvel endpoint en lecture seule, pas de modification du Principal ni du workspace context ni des plans

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — nouvel endpoint, pas de modification des flux existants

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `CaseFileStatsService` dans le package `casefile` (dépend des repos `analysis` → injection acceptable, pas de cycle)
- Isolation workspace : même pattern opaque que `CaseFileService.getById` (404 au lieu de 403)
- `totalTokens` agrège tous les job types (CHUNK_ANALYSIS, DOCUMENT_ANALYSIS, CASE_ANALYSIS, etc.)
