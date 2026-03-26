# Mini-spec — F-53 / SF-53-01 — Backend gestion statut des dossiers

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-53 / SF-53-01`

## Feature parente

`F-53` — Gestion du statut des dossiers

## Statut

`in-progress`

## Date de création

2026-03-26

## Branche Git

`feat/SF-53-01-backend-statut-dossier`

---

## Objectif

Permettre la clôture (tous membres), la réouverture (OWNER/ADMIN, avec gate quota) et la suppression soft (OWNER uniquement) d'un dossier, avec traçabilité dans `audit_logs` et filtrage des dossiers supprimés dans tous les endpoints existants.

---

## Comportement attendu

### Cas nominal

**Clôture** (`PATCH /api/v1/case-files/{id}/close`) :
- Tout membre du workspace peut clôturer un dossier `OPEN`
- Status passe de `OPEN` → `CLOSED`
- Audit log : action `CASE_FILE_CLOSED`
- Idempotent si déjà `CLOSED`

**Réouverture** (`PATCH /api/v1/case-files/{id}/reopen`) :
- OWNER ou ADMIN uniquement
- Gate quota : si `countOPEN >= max` → 402
- Status passe de `CLOSED` → `OPEN`
- Audit log : action `CASE_FILE_REOPENED`
- Idempotent si déjà `OPEN`

**Suppression soft** (`DELETE /api/v1/case-files/{id}`) :
- OWNER uniquement
- 409 si analyse en cours (`PENDING` ou `PROCESSING`)
- Pose `deleted_at = now()`
- Audit log : action `CASE_FILE_DELETED`
- Retourne `204 No Content`

**Filtrage** : tous les endpoints `GET` existants excluent les dossiers avec `deleted_at IS NOT NULL` → 404 opaque si accès direct.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| Dossier inexistant ou autre workspace | Not found | 404 |
| Dossier déjà supprimé (`deleted_at` non null) | Not found | 404 |
| Clôture dossier déjà `CLOSED` | Idempotent — 200 | 200 |
| Réouverture dossier déjà `OPEN` | Idempotent — 200 | 200 |
| Réouverture quota atteint | Payment required | 402 |
| Réouverture par LAWYER/MEMBER | Forbidden | 403 |
| Suppression par ADMIN/LAWYER/MEMBER | Forbidden | 403 |
| Suppression analyse en cours (PENDING/PROCESSING) | Conflict | 409 |

---

## Critères d'acceptation

- [ ] `PATCH /{id}/close` → status `CLOSED`, audit `CASE_FILE_CLOSED`, accessible à tous les membres
- [ ] `PATCH /{id}/reopen` → status `OPEN`, audit `CASE_FILE_REOPENED`, OWNER/ADMIN uniquement
- [ ] `PATCH /{id}/reopen` → 402 si quota atteint
- [ ] `DELETE /{id}` → `deleted_at` posé, audit `CASE_FILE_DELETED`, OWNER uniquement, 204
- [ ] `DELETE /{id}` → 409 si analyse PENDING ou PROCESSING
- [ ] `GET /case-files` ne retourne pas les dossiers supprimés
- [ ] `GET /case-files/{id}` → 404 si supprimé
- [ ] Sous-endpoints (documents, analyses, stats, chat, SSE) → 404 si dossier supprimé
- [ ] Gate quota création mise à jour avec `deleted_at IS NULL`
- [ ] Isolation workspace : 404 si autre workspace

---

## Périmètre

### Hors scope

- Blocage des analyses sur dossier `CLOSED`
- Interface frontend (SF-53-02)
- Suppression physique
- Restauration d'un dossier supprimé

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| PATCH | `/api/v1/case-files/{id}/close` | Oui | MEMBER |
| PATCH | `/api/v1/case-files/{id}/reopen` | Oui | ADMIN |
| DELETE | `/api/v1/case-files/{id}` | Oui | OWNER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_files` | ALTER + UPDATE | Ajout `deleted_at`, UPDATE sur `status` et `deleted_at` |
| `audit_logs` | INSERT | Actions : `CASE_FILE_CLOSED`, `CASE_FILE_REOPENED`, `CASE_FILE_DELETED` |

### Migration Liquibase

- [x] Oui — `028-case-file-status-soft-delete.xml`

---

## Plan de test

### Tests unitaires

- [ ] `CaseFileStatusService` — close nominal
- [ ] `CaseFileStatusService` — close idempotent
- [ ] `CaseFileStatusService` — reopen nominal
- [ ] `CaseFileStatusService` — reopen quota atteint → 402
- [ ] `CaseFileStatusService` — reopen LAWYER → 403
- [ ] `CaseFileStatusService` — delete nominal → deleted_at posé
- [ ] `CaseFileStatusService` — delete ADMIN → 403
- [ ] `CaseFileStatusService` — delete analyse en cours → 409
- [ ] `CaseFileStatusService` — 404 autre workspace

### Tests d'intégration

- [ ] I-01 PATCH /close → 200, status CLOSED
- [ ] I-02 PATCH /reopen → 200, status OPEN
- [ ] I-03 PATCH /reopen → 402 quota atteint
- [ ] I-04 PATCH /reopen → 403 rôle insuffisant
- [ ] I-05 DELETE → 204, deleted_at non null
- [ ] I-06 DELETE → 403 non OWNER
- [ ] I-07 DELETE → 409 analyse en cours
- [ ] I-08 GET /case-files → dossier supprimé absent
- [ ] I-09 GET /case-files/{id} → 404 si supprimé
- [ ] I-10 GET /case-files/{id} → 404 autre workspace

### Isolation workspace

- [x] Applicable

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — gate quota réouverture + mise à jour gate création
- [x] **Workspace context** — résolution rôle OWNER/ADMIN

### Composants impactés

| Composant | Impact |
|-----------|--------|
| `CaseFileService.create` | Quota query → `countByWorkspace_IdAndStatusAndDeletedAtIsNull` |
| `CaseFileService.list` | `findByWorkspaceAndDeletedAtIsNull` |
| `CaseFileService.getById` | `findByIdAndDeletedAtIsNull` |
| `CaseFileStatsService` | `findByIdAndDeletedAtIsNull` |
| `CaseAnalysisQueryService` | `findByIdAndDeletedAtIsNull` |
| `AnalysisJobQueryService` | `findByIdAndDeletedAtIsNull` |
| `ReAnalysisCommandService` | `findByIdAndDeletedAtIsNull` |
| `AiQuestionQueryService` | `findByIdAndDeletedAtIsNull` |
| `AnalysisStatusStreamController` | `findByIdAndDeletedAtIsNull` |
| `UsageEventQueryService` | `findByIdAndDeletedAtIsNull` |
| `ChatService` | `findByIdAndDeletedAtIsNull` |
| `DocumentService` | `findByIdAndDeletedAtIsNull` |
| `DocumentDeleteService` | `findByIdAndDeletedAtIsNull` |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Notes et décisions

- Status `OPEN` conservé (pas de renommage)
- Soft delete via `deleted_at` (timestamp), pas un statut
- 409 bloquant si analyse PENDING ou PROCESSING
- 402 sur réouverture si quota atteint — message "Limite de dossiers actifs atteinte. Passez à un plan supérieur."
