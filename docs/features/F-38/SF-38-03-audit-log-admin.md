# Mini-spec — F-38 / SF-38-03 Journal d'actions (audit log) dans l'Administration

## Identifiant
`F-38 / SF-38-03`

## Feature parente
`F-38` — Suppression de documents

## Statut
`ready`

## Date de création
2026-03-24

## Branche Git
`feat/SF-38-03-audit-log-admin`

---

## Objectif

Permettre aux OWNER et ADMIN de consulter le journal des actions sensibles (suppressions de documents) depuis l'écran Administration.

---

## Comportement attendu

### Cas nominal

**Backend :**
- `GET /api/v1/admin/audit-logs` → 50 dernières entrées du workspace, triées `created_at DESC`
- Accès OWNER/ADMIN uniquement → 403 sinon
- Réponse : `[{ id, action, userEmail, caseFileId, caseFileTitle, createdAt }]`

**Frontend :**
- Nouvelle section "Journal d'actions" dans WorkspaceAdminComponent
- Table : Date / Action / Utilisateur / Dossier
- Liste vide → "Aucune action enregistrée."

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| 403 | Section masquée (accessDenied déjà géré) |
| 500 | Snackbar "Erreur lors du chargement du journal." |
| Liste vide | Message "Aucune action enregistrée." |

---

## Critères d'acceptation

- [ ] GET /api/v1/admin/audit-logs → 200 OWNER/ADMIN, 403 MEMBER
- [ ] Réponse filtrée par workspace_id de l'utilisateur courant
- [ ] Table visible dans Administration (Date / Action / Utilisateur / Dossier)
- [ ] Tri created_at DESC
- [ ] Section absente si accessDenied()

---

## Périmètre

### Hors scope
- Pagination (50 entrées max pour V1)
- Filtrage / export CSV
- Autres types d'actions audit

---

## Technique

### Endpoint
| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/admin/audit-logs` | Oui | ADMIN |

### Composants Java
- `AuditLogRepository` — findByWorkspaceIdOrderByCreatedAtDesc
- `AuditLogAdminService` — résout workspace, vérifie rôle, joint user email, extrait caseFileTitle du metadata JSON
- `AuditLogAdminController` — GET endpoint
- `AuditLogResponse` — record de réponse

### Composants Angular
- `AuditLogEntry` model
- `AuditLogService`
- `WorkspaceAdminComponent` — nouvelle section

### Migration Liquibase
Non applicable.

---

## Plan de test

### Tests unitaires (backend)
- [ ] `AuditLogAdminService` — OWNER voit les logs de son workspace
- [ ] `AuditLogAdminService` — MEMBER → 403
- [ ] `AuditLogAdminService` — isolation workspace : logs d'un autre workspace non retournés

### Tests unitaires (frontend Karma)
- [ ] `WorkspaceAdminComponent` — section audit visible si auditLogs non vide
- [ ] `WorkspaceAdminComponent` — message "Aucune action" si liste vide

### Isolation workspace
- [x] Applicable — filtre strict sur workspace_id

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Aucune** — nouvel endpoint + nouvelle section, pas de routing ni d'auth modifiés

---

## Dépendances
- SF-38-01 — statut : done ✓
