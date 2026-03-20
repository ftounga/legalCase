# Mini-spec — F-25 / SF-25-03 Suppression workspace (super-admin)

## Identifiant

`F-25 / SF-25-03`

## Feature parente

`F-25` — Super-admin plateforme

## Statut

`ready`

## Date de création

2026-03-20

## Branche Git

`feat/SF-25-03-delete-workspace`

---

## Objectif

Exposer `DELETE /api/v1/super-admin/workspaces/{id}` supprimant un workspace et toutes ses données dépendantes en cascade, de façon atomique, avec annulation Stripe fail-open.

---

## Comportement attendu

### Cas nominal

Un super-admin appelle `DELETE /api/v1/super-admin/workspaces/{id}`. Toutes les données dépendantes sont supprimées dans une transaction unique dans cet ordre : `usage_events`, `ai_question_answers`, `ai_questions`, `case_analyses`, `analysis_jobs`, `document_analyses`, `chunk_analyses`, `document_chunks`, `document_extractions`, `documents`, `case_files`, `workspace_invitations`, `workspace_members`, `subscriptions` (+ annulation Stripe si stripeSubscriptionId présent, fail-open), puis `workspaces`. Retourne `204 No Content`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Workspace inexistant | Not found | 404 |
| Non `is_super_admin` | Accès refusé | 403 |
| Non authentifié | Non autorisé | 401 |
| Erreur Stripe lors de l'annulation | Suppression continue (fail-open) | 204 |

---

## Critères d'acceptation

- [ ] `DELETE /api/v1/super-admin/workspaces/{id}` retourne 204 avec un super-admin
- [ ] Toutes les entités dépendantes sont supprimées en cascade dans une transaction
- [ ] 404 si workspace inexistant
- [ ] 403 si `is_super_admin = false`
- [ ] Erreur Stripe → suppression continue, retourne quand même 204 (fail-open)

---

## Périmètre

### Hors scope

- Suppression d'utilisateur (SF-25-04)
- Frontend (SF-25-05)
- Rollback si erreur partielle post-Stripe

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| DELETE | `/api/v1/super-admin/workspaces/{id}` | Oui | `is_super_admin = true` |

### Tables impactées (ordre de suppression)

| Table | Opération | Notes |
|-------|-----------|-------|
| usage_events | DELETE WHERE case_file_id IN (...) | Via case_files du workspace |
| ai_question_answers | DELETE WHERE ai_question_id IN (...) | Via ai_questions du workspace |
| ai_questions | DELETE WHERE case_file_id IN (...) | |
| case_analyses | DELETE WHERE case_file_id IN (...) | |
| analysis_jobs | DELETE WHERE case_file_id IN (...) | |
| document_analyses | DELETE WHERE document_id IN (...) | Via documents du workspace |
| chunk_analyses | DELETE WHERE document_chunk_id IN (...) | Via chunks du workspace |
| document_chunks | DELETE WHERE document_id IN (...) | |
| document_extractions | DELETE WHERE document_id IN (...) | |
| documents | DELETE WHERE workspace_id = ... | |
| case_files | DELETE WHERE workspace_id = ... | |
| workspace_invitations | DELETE WHERE workspace_id = ... | |
| workspace_members | DELETE WHERE workspace_id = ... | |
| subscriptions | DELETE WHERE workspace_id = ... | + annulation Stripe fail-open |
| workspaces | DELETE WHERE id = ... | |

### Migration Liquibase

- [x] Non applicable

### Composants Angular (si applicable)

N/A — backend uniquement

---

## Plan de test

### Tests unitaires

- [ ] `SuperAdminService.deleteWorkspace()` — workspace avec case files et documents → toutes les entités supprimées dans l'ordre
- [ ] `SuperAdminService.deleteWorkspace()` — workspace inexistant → 404

### Tests d'intégration

- [ ] `DELETE /api/v1/super-admin/workspaces/{id}` → 204, workspace + case files supprimés
- [ ] `DELETE /api/v1/super-admin/workspaces/{uuid-inexistant}` → 404
- [ ] `DELETE /api/v1/super-admin/workspaces/{id}` avec non super-admin → 403

### Isolation workspace

- [ ] Non applicable — c'est précisément une opération cross-workspace intentionnelle

---

## Dépendances

### Subfeatures bloquantes

- SF-25-01 — statut : done
- SF-25-02 — statut : done

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- La suppression est transactionnelle — si une étape échoue, tout est rollbacké (sauf Stripe qui est fail-open)
- L'annulation Stripe est tentée avant la suppression de la subscription en base, et un échec Stripe est loggué mais n'interrompt pas la transaction
- L'ordre de suppression respecte les contraintes FK (feuilles d'abord)
- Les repositories existants sont réutilisés — pas de nouveau repository créé
