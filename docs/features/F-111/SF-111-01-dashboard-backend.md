# Mini-spec — F-111 / SF-111-01 : Backend dashboard opérationnel workspace

## Identifiant
`F-111 / SF-111-01`

## Feature parente
`F-111` — Tableau de bord opérationnel workspace

## Statut
`in-progress`

## Date de création
`2026-04-04`

## Branche Git
`feat/SF-111-01-dashboard-backend`

---

## Objectif
Exposer un endpoint `GET /api/v1/dashboard` qui retourne en un seul appel les données agrégées du workspace nécessaires au tableau de bord : dossiers ouverts, délais urgents cross-dossiers, alertes checklist en retard, et activité récente.

---

## Comportement attendu

### Cas nominal
1. L'utilisateur authentifié appelle `GET /api/v1/dashboard`.
2. Le backend résout son workspace, récupère les données en DB et retourne `DashboardSummaryResponse` avec :
   - `openCases` : liste des dossiers actifs (deletedAt IS NULL, status ≠ CLOSED), limité à 20, triés par `createdAt DESC` — champs : id, title, legalDomain, riskLevel, riskScore
   - `openCasesCount` : nombre total de dossiers actifs du workspace
   - `urgentDeadlines` : délais dont `dueDate ≤ today + 7 days`, issus des dossiers actifs du workspace — champs : id, label, dueDate, caseFileId, caseFileTitle
   - `staleChecks` : dossiers ayant au moins 1 `ProcedureCheck` avec `statut = NON_COMPLIANT` dont `updatedAt < now - 7 jours` — champs : caseFileId, caseFileTitle, nonCompliantCount
   - `recentAnalyses` : 5 dernières analyses `DONE` du workspace, triées par `createdAt DESC` — champs : id, caseFileId, caseFileTitle, analysisType, createdAt

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| Utilisateur non authentifié | 401 | 401 |
| Workspace introuvable | 404 | 404 |

---

## Critères d'acceptation

- [x] `GET /api/v1/dashboard` → 200 avec `DashboardSummaryResponse`
- [x] `openCases` : uniquement les dossiers actifs (deletedAt IS NULL, status ≠ CLOSED) du workspace, max 20
- [x] `openCasesCount` : compte total des dossiers actifs du workspace
- [x] `urgentDeadlines` : délais `dueDate ≤ today + 7 days` uniquement pour les dossiers du workspace (isolation workspace)
- [x] `staleChecks` : NON_COMPLIANT avec `updatedAt < now - 7j` — groupés par dossier
- [x] `recentAnalyses` : 5 dernières DONE du workspace, triées par `createdAt DESC`
- [x] Isolation workspace : aucune donnée d'un autre workspace dans la réponse

---

## Périmètre

### Hors scope
- Composant Angular (SF-111-02)
- Agrégation des uploads
- Filtrage par domaine juridique
- Pagination des sections

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| GET | `/api/v1/dashboard` | Oui | Tout membre |

### Nouvelles classes backend
- `DashboardSummaryResponse` (record : openCases, openCasesCount, urgentDeadlines, staleChecks, recentAnalyses)
- `DashboardOpenCaseItem` (record : id, title, legalDomain, riskLevel, riskScore)
- `DashboardDeadlineItem` (record : id, label, dueDate, caseFileId, caseFileTitle)
- `DashboardStaleCheckItem` (record : caseFileId, caseFileTitle, nonCompliantCount)
- `DashboardAnalysisItem` (record : id, caseFileId, caseFileTitle, analysisType, createdAt)
- `DashboardService`
- `DashboardController`

### Repositories modifiés
- `CaseFileRepository` : `findTop20ByWorkspaceAndDeletedAtIsNullAndStatusNotOrderByCreatedAtDesc` + `countByWorkspaceAndDeletedAtIsNullAndStatusNot`
- `CaseDeadlineRepository` : `@Query` urgent deadlines cross-workspace avec JOIN
- `ProcedureCheckRepository` : `@Query` stale NON_COMPLIANT groupés par dossier
- `CaseAnalysisRepository` : `findTop5ByCaseFile_WorkspaceAndAnalysisStatusOrderByCreatedAtDesc`

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| case_files | SELECT | lecture seule |
| case_deadlines | SELECT | lecture seule |
| procedure_checks | SELECT | lecture seule |
| case_analyses | SELECT | lecture seule |

---

## Plan de test

### Backend — unitaires (DASH)
- [x] DASH-01 : `buildSummary()` — workspace vide → toutes les listes vides, openCasesCount = 0
- [x] DASH-02 : `urgentDeadlines` — délai à J+6 inclus, délai à J+8 exclu
- [x] DASH-03 : `staleChecks` — check NON_COMPLIANT à J-8 inclus, check à J-6 exclu
- [x] DASH-04 : `openCases` — dossier CLOSED exclu, dossier deletedAt non null exclu

### Backend — intégration (IT-DASH)
- [x] IT-DASH-01 : GET 200, structure complète
- [x] IT-DASH-02 : isolation workspace — données workspace B absentes de la réponse workspace A

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Workspace context** — résolution workspace via OAuthProviderResolver (pattern existant)

| Composant | Impact potentiel | Non-régression |
|-----------|-----------------|----------------|
| `CaseFileRepository` | nouvelles méthodes dérivées JPA | Méthodes existantes inchangées |
| `CaseDeadlineRepository` | nouvelle query JPQL | findByCaseFileIdOrderByDueDateAsc inchangé |
| `ProcedureCheckRepository` | nouvelle query JPQL | findByCaseAnalysisIdOrderByOrdreAsc inchangé |
| `CaseAnalysisRepository` | nouvelle méthode dérivée | Méthodes existantes inchangées |

---

## Dépendances
- SF-96-01 (procedure_checks) — Done
- SF-69-01 (case_deadlines) — Done
- F-53 (statut ACTIVE/CLOSED) — Done
